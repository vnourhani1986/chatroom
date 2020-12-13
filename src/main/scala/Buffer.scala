import cats.effect._
import cats.syntax.all._
import cats.effect.concurrent.Ref

trait Buffer[F[_], T] {
  def get: F[List[T]]
  def modify(value: T): F[List[T]]
}

final class StringBuffer[F[_]: Concurrent](
    ref: Ref[F, List[String]],
    size: Int
) extends Buffer[F, String] {
  def get: F[List[String]] = ref.get
  def modify(value: String): F[List[String]] =
    for {
      list <- get
      len <- Sync[F].delay(list.length)
      res <-
        if (len < size) ref.modify(l => (l ++ List(value), l ++ List(value)))
        else
          ref.modify(l => (l.drop(1) ++ List(value), l.drop(1) ++ List(value)))
    } yield res
}

object Buffer {
  def apply[F[_]: Concurrent](size: Int): F[Buffer[F, String]] =
    for {
      ref <- Ref.of[F, List[String]](List.empty)
    } yield new StringBuffer[F](ref, size)
}
