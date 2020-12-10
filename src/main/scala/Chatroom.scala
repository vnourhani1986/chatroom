import org.http4s._
import cats.implicits._
import cats.syntax.all._
import cats.effect._
import cats.effect.concurrent.Ref

import org.http4s.websocket.WebSocketFrame

import fs2._
import fs2.concurrent.Topic

object Chatroom extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {

    import scala.concurrent.ExecutionContext.Implicits.global
  
    (for {                  
      webSocketHandler <- Stream.eval(WebSocketHandler[IO])
      exitCode <- webSocketHandler.server(9002)
    } yield exitCode).compile.last.map(_.get)

  }

}
