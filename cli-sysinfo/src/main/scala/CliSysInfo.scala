import cats.implicits._
import cats.syntax.all._
import cats.effect._

import scala.concurrent.duration._

import fs2._

object CliSysInfo extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    Stream
      .eval(
        SysInfoImpl[IO].build(
          Stream
            .repeatEval(IO.delay("hello"))
            .delayBy(1.second)
            .metered(1.second)
        )
      )
      .compile
      .drain      
      .as(ExitCode.Success)
}
