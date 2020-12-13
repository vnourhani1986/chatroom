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

case class TopicDataModel(user: String, room: String, message: String)

trait WebSocketHandler[F[_]] extends Http4sDsl[F] {
  def routes(maxQueued: Int): HttpRoutes[F]
  def server(port: Int)(implicit ex: ExecutionContext): Stream[F, ExitCode]
}
class WebSocketHandlerImpl[F[_]: Sync: ConcurrentEffect: Timer](
    topicFactory: TopicFactory[F, String],
    bufferFactory: BufferFactory[F, String]
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
            buffer <- bufferFactory.get(room)
            publish <- topic.publish1(
              TopicDataModel(
                user,
                room,
                s"left the room"
              ).asJson.show
            )
            _ <-
              buffer.modify(
                TopicDataModel(
                  user,
                  room,
                  s"left the room"
                ).asJson.show
              )
          } yield publish
        )
      }

  def server(port: Int)(implicit ec: ExecutionContext): Stream[F, ExitCode] =
    BlazeServerBuilder[F](ec)
      .bindHttp(port)
      .withHttpApp(routes(100).orNotFound)
      .serve

}

object WebSocketHandler {
  def apply[F[_]: Sync: ConcurrentEffect: Timer]: F[WebSocketHandler[F]] =
    for {
      topicFactory <- TopicFactory[F]
      bufferFactory <- BufferFactory[F](100)
      webSocketHandler <- Sync[F].delay(
        new WebSocketHandlerImpl[F](topicFactory, bufferFactory)
      )
    } yield webSocketHandler

}
