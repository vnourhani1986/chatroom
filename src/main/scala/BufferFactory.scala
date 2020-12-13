import cats.effect._
import cats.syntax.all._
import cats.effect.concurrent.Ref

trait BufferFactory[F[_], T] {
  def get(key: String): F[Buffer[F, T]]
}

class BufferFactoryImpl[F[_]: Concurrent, T](
    buffers: Ref[F, Map[String, Buffer[F, T]]],
    size: Int
) extends BufferFactory[F, T] {
  def get(key: String): F[Buffer[F, T]] =
    for {
      bufferMap <- buffers.get
      bufferOpt <- Sync[F].delay(bufferMap.get(key))
      buffer <- bufferOpt match {
        case None =>
          for {
            t <- Buffer[F, T](size)
            _ <- buffers.modify(ts => (ts.+(key -> t), ts))
          } yield t
        case Some(value) => Sync[F].delay(value)
      }
    } yield buffer
}

object BufferFactory {
  def apply[F[_]: Concurrent, T](size: Int): F[BufferFactory[F, T]] =
    for {
      buffers <- Ref.of[F, Map[String, Buffer[F, T]]](Map.empty)
      factory <- Sync[F].delay(new BufferFactoryImpl[F, T](buffers, size))
    } yield factory
}