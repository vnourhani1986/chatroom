import fs2._
import cats.effect._

object SysInfoImpl {
  def apply[F[_]: Sync]: SysInfo[F, Stream[F, Unit]] =
    new SysInfo[F, Stream[F, Unit]] {

      def build[A](stream: Stream[F, A])(implicit
          aToString: A => String,
          stringToA: String => A
      ): F[Stream[F, Unit]] =
        Sync[F].delay(show(stream).map(_ => ()))

      def show[A](stream: Stream[F, A]): Stream[F, A] =
        for {
          value <- stream
          _ <- Stream.eval(Sync[F].delay(println(value)))
        } yield value

    }
}
