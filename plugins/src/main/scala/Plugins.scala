import fs2._

object Plugins {
  type Plugin[F[_]] = Pipe[F, String, String]

  object EchoPlugin {
    def apply[F[_]]: Plugin[F] = identity
  }

}
