import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl.io._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame.Text
import org.http4s.circe._

import io.circe.syntax._
import io.circe.generic.auto._
import io.circe._, io.circe.parser._

import cats.effect._
import cats.syntax.all._
import cats.effect.concurrent.Ref

import fs2.{Pipe, Stream}
import fs2.concurrent.Topic
import org.http4s.server.blaze.BlazeServerBuilder
import scala.concurrent.ExecutionContext
import io.circe.Json

trait TopicFactory[F[_], T] {
  def get(key: String): F[Topic[F, T]]
}

class StringTopicFactory[F[_]: Sync: Concurrent](
    topics: Ref[F, Map[String, Topic[F, String]]]
) extends TopicFactory[F, String] {
  def get(key: String): F[Topic[F, String]] =
    for {
      topicMap <- topics.get
      topicOpt <- Sync[F].delay(topicMap.get(key))
      topic <- topicOpt match {
        case None =>
          for {
            t <- Topic[F, String](s"welcome to $key")
            _ <- topics.modify(ts => (ts.+(key -> t), ts))
          } yield t
        case Some(value) => Sync[F].delay(value)
      }
    } yield topic
}

object TopicFactory {
  def apply[F[_]: Sync: Concurrent]: F[TopicFactory[F, String]] =
    for {
      topics <- Ref.of[F, Map[String, Topic[F, String]]](Map.empty)
      factory <- Sync[F].delay(new StringTopicFactory[F](topics))
    } yield factory
}

trait BufferFactory[F[_], T] {
  def get(key: String): F[Buffer[F, T]]
}

class StringBufferFactory[F[_]: Sync: Concurrent](
    buffers: Ref[F, Map[String, Buffer[F, String]]],
    size: Int
) extends BufferFactory[F, String] {
  def get(key: String): F[Buffer[F, String]] =
    for {
      bufferMap <- buffers.get
      bufferOpt <- Sync[F].delay(bufferMap.get(key))
      buffer <- bufferOpt match {
        case None =>
          for {
            t <- Buffer[F](size)
            _ <- buffers.modify(ts => (ts.+(key -> t), ts))
          } yield t
        case Some(value) => Sync[F].delay(value)
      }
    } yield buffer
}

object BufferFactory {
  def apply[F[_]: Sync: Concurrent](size: Int): F[BufferFactory[F, String]] =
    for {
      buffers <- Ref.of[F, Map[String, Buffer[F, String]]](Map.empty)
      factory <- Sync[F].delay(new StringBufferFactory[F](buffers, size))
    } yield factory
}

trait Buffer[F[_], T] {
  def get: F[List[T]]
  def modify(value: T): F[List[T]]
}

final class StringBuffer[F[_]: Sync: Concurrent](
    ref: Ref[F, List[String]],
    size: Int
) extends Buffer[F, String] {
  def get: F[List[String]] = ref.get
  def modify(value: String): F[List[String]] =
    for {
      list <- get
      len <- Sync[F].delay(list.length)
      res <-
        if (len < size) ref.modify(l => (l ++ List(value), l ++ List(value)))
        else
          ref.modify(l => (l.drop(1) ++ List(value), l.drop(1) ++ List(value)))
    } yield res
}

object Buffer {
  def apply[F[_]: Sync: Concurrent](size: Int): F[Buffer[F, String]] =
    for {
      ref <- Ref.of[F, List[String]](List.empty)
    } yield new StringBuffer[F](ref, size)
}

case class TopicDataModel(user: String, room: String, message: String)

trait WebSocketHandler[F[_]] extends Http4sDsl[F] {
  def routes(maxQueued: Int): HttpRoutes[F]
  def server(port: Int): Stream[F, ExitCode]
}
class WebSocketHandlerImpl[F[_]: Sync: ConcurrentEffect: Timer](
    topicFactory: TopicFactory[F, String],
    bufferFactory: BufferFactory[F, String]
)(implicit
    ec: ExecutionContext
) extends WebSocketHandler[F] {

  implicit val topicDataModelDecoder = jsonOf[F, TopicDataModel]

  def routes(maxQueued: Int): HttpRoutes[F] =
    HttpRoutes
      .of[F] { case GET -> Root / "rooms" / room / user =>
        WebSocketBuilder[F].build(
          for {
            topic <- Stream.eval(topicFactory.get(room))
            buffer <- Stream.eval(bufferFactory.get(room))
            list <- Stream.eval(buffer.get)
            subscriber <- Stream.emits(list.slice(0, list.length - 1)) ++ topic
              .subscribe(maxQueued)
            data <- Stream
              .eval(
                Sync[F].delay(
                  parse(subscriber) match {
                    case Right(json) =>
                      json.as[TopicDataModel].toOption match {
                        case Some(value) =>
                          Stream(
                            s"${value.user} in ${value.room}: ${value.message}"
                          )
                        case None => Stream.empty
                      }
                    case Left(error) => Stream.empty
                  }
                )
              )
              .flatten
            res <- Stream.eval(Sync[F].delay(Text(data)))
          } yield res,
          _.flatMap(webSocketFrame =>
            for {
              topic <- Stream.eval(topicFactory.get(room))
              buffer <- Stream.eval(bufferFactory.get(room))
              publish <- Stream.eval(
                topic.publish1(
                  TopicDataModel(
                    user,
                    room,
                    webSocketFrame.data.toArray.map(_.toChar).mkString
                  ).asJson.show
                )
              )
              _ <- Stream.eval(
                buffer.modify(
                  TopicDataModel(
                    user,
                    room,
                    webSocketFrame.data.toArray.map(_.toChar).mkString
                  ).asJson.show
                )
              )
            } yield publish
          ),
          onClose = for {
            topic <- topicFactory.get(room)
            publish <- topic.publish1(
              TopicDataModel(
                user,
                room,
                s"left the room"
              ).asJson.show
            )
          } yield publish
        )
      }

  def server(port: Int): Stream[F, ExitCode] =
    BlazeServerBuilder[F](ec)
      .bindHttp(port)
      .withHttpApp(routes(100).orNotFound)
      .serve

}

object WebSocketHandler {
  def apply[F[_]: Sync: ConcurrentEffect: Timer](implicit
      ex: ExecutionContext
  ): F[WebSocketHandler[F]] =
    for {
      topicFactory <- TopicFactory[F]
      bufferFactory <- BufferFactory[F](100)
      webSocketHandler <- Sync[F].delay(
        new WebSocketHandlerImpl[F](topicFactory, bufferFactory)
      )
    } yield webSocketHandler

}
