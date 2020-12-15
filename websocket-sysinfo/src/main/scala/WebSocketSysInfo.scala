import fs2._
import cats.effect._
import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl.io._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame.Text
import org.http4s.websocket.WebSocketFrame
import org.http4s.circe._

object SysInfoImpl {
  def apply[F[_]: Concurrent]: SysInfo[F, Response[F]] =
    new SysInfo[F, Response[F]] {
      def build[A](stream: Stream[F, A])(implicit
          aToString: A => String,
          stringToA: String => A
      ): F[Response[F]] =
        WebSocketBuilder[F].build(
          streamEncoder(stream),
          in => streamDecoder[A](in).map(_ => Unit)
        )

      def streamEncoder[A](stream: Stream[F, A])(implicit
          f: A => String
      ): Stream[F, WebSocketFrame] =
        stream.evalMap(s => Sync[F].delay(encoder[A](s)))

      def streamDecoder[A](stream: Stream[F, WebSocketFrame])(implicit
          f: String => A
      ): Stream[F, A] =
        stream.evalMap(s => Sync[F].delay(decoder[A](s)))

      def encoder[A](a: A)(implicit f: A => String): WebSocketFrame =
        Text(a)

      def decoder[A](frame: WebSocketFrame)(implicit f: String => A): A =
        frame.data.toArray.map(_.toChar).mkString
    }
}
