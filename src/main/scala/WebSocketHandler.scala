import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl.io._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame.Text

import cats.effect._
import cats.syntax.all._

import fs2._
import fs2.concurrent.Topic
import org.http4s.server.blaze.BlazeServerBuilder
import scala.concurrent.ExecutionContext

trait WebSocketHandler[F[_]] extends Http4sDsl[F] {
  def routes(maxQueued: Int): HttpRoutes[F]
  def server(port: Int): Stream[F, ExitCode]
}

class WebSocketHandlerImpl[F[_]: Sync: ConcurrentEffect: Timer](
    topic: Topic[F, String]
)(implicit
    ec: ExecutionContext
) extends WebSocketHandler[F] {

  def routes(maxQueued: Int): HttpRoutes[F] =
    HttpRoutes
      .of[F] { case GET -> Root / "rooms" / room / user =>
        WebSocketBuilder[F].build(
          topic
            .subscribe(maxQueued)
            .flatMap(string => Stream.eval(Sync[F].delay(Text(string)))),
          _.flatMap(webSocketFrame =>
            Stream.eval(
              topic.publish1(
                s"$user in $room: ${webSocketFrame.data.toArray.map(_.toChar).mkString}"
              )
            )
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
  def apply[F[_]: Sync: ConcurrentEffect: Timer](
      topic: Topic[F, String]
  )(implicit
      ex: ExecutionContext
  ): F[WebSocketHandler[F]] =
    Sync[F].delay(new WebSocketHandlerImpl[F](topic))
}
