import org.http4s._
import cats.implicits._
import cats.syntax.all._
import cats.effect._

import org.http4s.websocket.WebSocketFrame

import fs2._

import scala.concurrent.ExecutionContext.Implicits.global

object Chatroom extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {

    (for {
      port <- args.headOption match {
        case None =>
          // Stream.raiseError[IO](new Exception("server port is not defiend"))
          Stream(8080)
        case Some(value) => Stream(value.toInt)
      }
      webSocketHandler <- Stream.eval(WebSocketHandler[IO, String])
      exitCode <- webSocketHandler.server(port)
    } yield exitCode).compile.drain.as(ExitCode.Success)

  }

}
