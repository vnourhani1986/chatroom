import org.http4s._
import cats.implicits._
import cats.effect._

object Chatroom extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    IO { println(args) } >> IO.pure(ExitCode.Success)
}
