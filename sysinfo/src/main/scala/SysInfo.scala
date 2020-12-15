import fs2._

trait SysInfo[F[_], R] {
  def build[A](s1: Stream[F, A])(implicit
      aToString: A => String,
      stringToA: String => A
  ): F[R]
}
