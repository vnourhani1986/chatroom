import cats.effect._
import cats.syntax.all._
import cats.effect.concurrent.Ref

trait Buffer[F[_], T] {
  def get: F[List[T]]
  def modify(value: T): F[List[T]]
}

final class BufferImpl[F[_]: Concurrent, T](
    ref: Ref[F, List[T]],
    size: Int
) extends Buffer[F, T] {
  def get: F[List[T]] = ref.get
  def modify(value: T): F[List[T]] =
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
  def apply[F[_]: Concurrent, T](size: Int): F[Buffer[F, T]] =
    for {
      ref <- Ref.of[F, List[T]](List.empty)
    } yield new BufferImpl[F, T](ref, size)
}
