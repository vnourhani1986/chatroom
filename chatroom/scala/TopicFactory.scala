import cats.effect._
import cats.syntax.all._
import cats.effect.concurrent.Ref

import fs2.concurrent.Topic

trait TopicFactory[F[_], T] {
  def get(key: String, initial: T): F[Topic[F, T]]
}

class TopicFactoryImpl[F[_]: Concurrent, T](
    topics: Ref[F, Map[String, Topic[F, T]]]
) extends TopicFactory[F, T] {
  def get(key: String, initial: T): F[Topic[F, T]] =
    for {
      topicMap <- topics.get
      topicOpt <- Sync[F].delay(topicMap.get(key))
      topic <- topicOpt match {
        case None =>
          for {
            t <- Topic[F, T](initial)
            _ <- topics.modify(ts => (ts.+(key -> t), ts))
          } yield t
        case Some(value) => Sync[F].delay(value)
      }
    } yield topic
}

object TopicFactory {
  def apply[F[_]: Concurrent, T]: F[TopicFactory[F, T]] =
    for {
      topics <- Ref.of[F, Map[String, Topic[F, T]]](Map.empty)
      factory <- Sync[F].delay(new TopicFactoryImpl[F, T](topics))
    } yield factory
}
