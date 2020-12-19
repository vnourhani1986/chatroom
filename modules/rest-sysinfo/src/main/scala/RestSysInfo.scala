import fs2._
import cats.effect._
import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl.io._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame.Text
import org.http4s.websocket.WebSocketFrame
import org.http4s.circe._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.Router
import cats.syntax.all._

import fs2.concurrent.Queue

import Plugins._

object RestSysInfo {

  def apply[F[_]: Concurrent](
      plugin: Plugin[F],
      bufSize: Int
  ): F[HttpRoutes[F]] =
    for {
      queue <- Queue.bounded[F, String](bufSize)
      routes <- Sync[F].delay(HttpRoutes.of[F] {
        case req @ POST -> Root =>
          req.as[String].map { body =>
            Stream(body)
              .through(plugin)
              .through(queue.enqueue)
            Response[F](Status.Ok)
          }

        case GET -> Root =>
          queue.dequeue1
            .flatMap(body =>
              Sync[F].delay(Response[F](Status.Ok).withEntity(body))
            )
      })
    } yield routes

}
