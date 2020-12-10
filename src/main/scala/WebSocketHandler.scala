import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl.io._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame.Text

import cats.effect._
import cats.syntax.all._
import cats.effect.concurrent.Ref

import fs2._
import fs2.concurrent.Topic
import org.http4s.server.blaze.BlazeServerBuilder
import scala.concurrent.ExecutionContext

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

trait WebSocketHandler[F[_]] extends Http4sDsl[F] {
  def routes(maxQueued: Int): HttpRoutes[F]
  def server(port: Int): Stream[F, ExitCode]
}
class WebSocketHandlerImpl[F[_]: Sync: ConcurrentEffect: Timer](
    topicFactory: TopicFactory[F, String]
)(implicit
    ec: ExecutionContext
) extends WebSocketHandler[F] {

  def routes(maxQueued: Int): HttpRoutes[F] =
    HttpRoutes
      .of[F] { case GET -> Root / "rooms" / room / user =>
        WebSocketBuilder[F].build(
          for {
            topic <- Stream.eval(topicFactory.get(room))
            string <- topic.subscribe(maxQueued)
            res <- Stream.eval(Sync[F].delay(Text(string)))
          } yield res,
          _.flatMap(webSocketFrame =>
            for {
              topic <- Stream.eval(topicFactory.get(room))
              publish <- Stream.eval(
                topic.publish1(
                  s"$user in $room: ${webSocketFrame.data.toArray.map(_.toChar).mkString}"
                )
              )
            } yield publish
          )
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
      webSocketHandler <- Sync[F].delay(
        new WebSocketHandlerImpl[F](topicFactory)
      )
    } yield webSocketHandler

}
