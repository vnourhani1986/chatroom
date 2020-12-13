import cats.effect._
import cats.syntax.all._
import cats.effect.concurrent.Ref

trait BufferFactory[F[_], T] {
  def get(key: String): F[Buffer[F, T]]
}

class StringBufferFactory[F[_]: Concurrent](
    buffers: Ref[F, Map[String, Buffer[F, String]]],
    size: Int
) extends BufferFactory[F, String] {
  def get(key: String): F[Buffer[F, String]] =
    for {
      bufferMap <- buffers.get
      bufferOpt <- Sync[F].delay(bufferMap.get(key))
      buffer <- bufferOpt match {
        case None =>
          for {
            t <- Buffer[F](size)
            _ <- buffers.modify(ts => (ts.+(key -> t), ts))
          } yield t
        case Some(value) => Sync[F].delay(value)
      }
    } yield buffer
}

object BufferFactory {
  def apply[F[_]: Concurrent](size: Int): F[BufferFactory[F, String]] =
    for {
      buffers <- Ref.of[F, Map[String, Buffer[F, String]]](Map.empty)
      factory <- Sync[F].delay(new StringBufferFactory[F](buffers, size))
    } yield factory
}