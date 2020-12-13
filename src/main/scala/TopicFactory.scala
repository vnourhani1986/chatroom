import cats.effect._
import cats.syntax.all._
import cats.effect.concurrent.Ref

import fs2.concurrent.Topic

trait TopicFactory[F[_], T] {
  def get(key: String): F[Topic[F, T]]
}

class StringTopicFactory[F[_]: Concurrent](
    topics: Ref[F, Map[String, Topic[F, String]]]
) extends TopicFactory[F, String] {
  def get(key: String): F[Topic[F, String]] =
    for {
      topicMap <- topics.get
      topicOpt <- Sync[F].delay(topicMap.get(key))
      topic <- topicOpt match {
        case None =>
          for {
            t <- Topic[F, String](s"welcome to $key")
            _ <- topics.modify(ts => (ts.+(key -> t), ts))
          } yield t
        case Some(value) => Sync[F].delay(value)
      }
    } yield topic
}

object TopicFactory {
  def apply[F[_]: Concurrent]: F[TopicFactory[F, String]] =
    for {
      topics <- Ref.of[F, Map[String, Topic[F, String]]](Map.empty)
      factory <- Sync[F].delay(new StringTopicFactory[F](topics))
    } yield factory
}
