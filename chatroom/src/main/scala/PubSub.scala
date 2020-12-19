import cats.effect._
import cats.syntax.all._
import cats.effect.concurrent.Ref

import fs2._
import fs2.concurrent.Topic

trait PubSub[F[_], T] {
  def subscribe[A](key: String)(implicit decoder: T => A): Stream[F, A]
  def publish[A](key: String)(implicit encoder: A => T): Pipe[F, A, A]
}

final class PubSubImpl[F[_]: Sync, T](
    topicFactory: TopicFactory[F, T],
    bufferFactory: BufferFactory[F, T],
    initial: T
) extends PubSub[F, T] {

  def subscribe[A](key: String)(implicit decoder: T => A): Stream[F, A] =
    for {
      topic <- Stream.eval(topicFactory.get(key, initial))
      buffer <- Stream.eval(bufferFactory.get(key))
      list <- Stream.eval(buffer.get)
      subscriber <- Stream.emits(
        list.slice(0, list.length - 1)
      ) ++ topic
        .subscribe(100)
    } yield subscriber

  def publish[A](key: String)(implicit encoder: A => T): Pipe[F, A, A] =
    _.evalMap { data =>
      for {
        topic <- topicFactory.get(key, initial)
        buffer <- bufferFactory.get(key)
        _ <- topic.publish1(data)
        _ <- buffer.modify(data)
      } yield data
    }
}

object PubSub {
  def apply[F[_]: Concurrent, T](size: Int, initial: T): F[PubSub[F, T]] =
    for {
      topicFactory <- TopicFactory[F, T]
      bufferFactory <- BufferFactory[F, T](size)
      pubSub <- Sync[F].delay(
        new PubSubImpl[F, T](topicFactory, bufferFactory, initial)
      )
    } yield pubSub
}
