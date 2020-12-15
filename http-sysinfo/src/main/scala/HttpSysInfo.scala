import cats.implicits._
import cats.syntax.all._
import cats.effect._

import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl.io._
import org.http4s.dsl.Http4sDsl

import scala.concurrent.duration._

import org.http4s.server.blaze.BlazeServerBuilder
import scala.concurrent.ExecutionContext

import fs2._

object HttpSysInfo extends IOApp {
  def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO](scala.concurrent.ExecutionContext.Implicits.global)
      .bindHttp(8080)
      .withHttpApp(
        route(
          Stream
            .repeatEval(IO.delay("hello"))
            .delayBy(1.second)
            .metered(1.second)
        ).orNotFound
      )
      .serve
      .compile
      .drain
      .as(ExitCode.Success)

  def route(stream: Stream[IO, String]) =
    HttpRoutes.of[IO] { case GET -> Root / "sysinfo" =>
      SysInfoImpl[IO].build[String](stream)
    }

}
