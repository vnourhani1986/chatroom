import fs2._
import cats.effect._
import cats.syntax.all._

import fs2.concurrent.Queue

import Plugins._

object CliSysInfo {
  def apply[F[_]: Concurrent: ContextShift](
      plugin: Plugin[F],
      bufSize: Int
  ): Stream[F, Unit] = {

    def stringToByte: Pipe[F, String, Byte] =
      _.flatMap(string => Stream.emits(string.getBytes))

    for {
      blocker <- Stream.resource(Blocker[F])
      queue <- Stream.eval(Queue.bounded[F, String](bufSize))
      in <- fs2.io
        .stdinUtf8(bufSize, blocker)
        .through(plugin)
        .through(queue.enqueue)
      out <- queue.dequeue
        .through(stringToByte)
        .through(fs2.io.stdout(blocker))
    } yield out
  }
}

