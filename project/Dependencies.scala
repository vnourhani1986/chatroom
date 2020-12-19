import sbt.{CrossVersion, compilerPlugin, _}

object Dependencies {

  object Versions {
    val circe = "0.12.3"
    val http4s = "0.20.23"
    val specs2 = "4.1.0"
    val logback = "1.2.3"
    val fs2 = "2.4.6"
    val pureconfig = "0.14.0"
  }

  val `cats-effect` = Seq(
    "org.typelevel" %% "cats-effect" % "2.2.0" withSources () withJavadoc (),
    "io.chrisdavenport" %% "cats-effect-time" % "0.1.2"
  )

  val `circe` = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % Versions.circe)

  val `http4s` = Seq(
    "org.http4s" %% "http4s-blaze-server",
    "org.http4s" %% "http4s-blaze-client",
    "org.http4s" %% "http4s-circe",
    "org.http4s" %% "http4s-dsl"
  ).map(_ % Versions.http4s)

  val `log` = Seq(
    "ch.qos.logback" % "logback-classic"
  ).map(_ % Versions.logback)

  val `test` = Seq(
    "org.specs2" %% "specs2-core"
  ).map(_ % Versions.specs2 % Test)

  val `pureconfig` = Seq(
    "com.github.pureconfig" %% "pureconfig",
    "com.github.pureconfig" %% "pureconfig-cats-effect"
  ).map(_ % Versions.pureconfig)

  val `fs2` = Seq(
    "co.fs2" %% "fs2-core",
    "co.fs2" %% "fs2-io",
    "co.fs2" %% "fs2-reactive-streams",
    "co.fs2" %% "fs2-experimental"
  ).map(_ % Versions.fs2)

}
