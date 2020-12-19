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

object WebSocketSysInfo {

  def apply[F[_]: Concurrent](
      plugin: Plugin[F],
      bufSize: Int
  ): F[HttpRoutes[F]] =
    HttpRoutes
      .of[F] { case GET -> Root =>
        for {
          queue <- Queue.bounded[F, String](bufSize)
          response <- WebSocketBuilder[F].build(
            queue.dequeue
              .through(plugin)
              .map(Text(_)),
            _.map { webSocketFrame =>
              Stream
                .eval(Sync[F].delay(webSocketFrame.data.toArray.mkString))
                .through {
                  _.map(queue.enqueue1)
                }
            }
          )
        } yield response
      }
      .pure

}
