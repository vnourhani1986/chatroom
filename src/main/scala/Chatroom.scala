import org.http4s._
import cats.implicits._
import cats.effect._
import org.http4s.websocket.WebSocketFrame

import fs2._
import fs2.concurrent.Topic

object Chatroom extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {

    import scala.concurrent.ExecutionContext.Implicits.global

    (for {
      topic <- Stream.eval(Topic[IO, String]("welcome to chatroom"))
      webSocketHandler <- Stream.eval(WebSocketHandler[IO](topic))
      exitCode <- webSocketHandler.server(9001)
    } yield exitCode).compile.last.map(_.get)

  }

}
