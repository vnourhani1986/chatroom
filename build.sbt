name := "chatroom"

version in ThisBuild := "2.2.0"

scalaVersion in ThisBuild := "2.12.8"

lazy val `sysinfo` = (project in file("sysinfo"))
  .settings(
    libraryDependencies ++= fs2
  )

lazy val `websocket-sysinfo` = (project in file("websocket-sysinfo"))
  .settings(
    libraryDependencies ++= fs2 ++ `cats-effect` ++ http4s
  )
  .dependsOn(`sysinfo`)

lazy val `console-sysinfo` = (project in file("console-sysinfo"))
  .settings(
    libraryDependencies ++= fs2 ++ `cats-effect`
  )
  .dependsOn(`sysinfo`)

lazy val `http-sysinfo` = (project in file("http-sysinfo"))
  .settings(
    libraryDependencies ++= fs2 ++ `cats-effect`
  )
  .dependsOn(`websocket-sysinfo`)

lazy val `cli-sysinfo` = (project in file("cli-sysinfo"))
  .settings(
    libraryDependencies ++= fs2 ++ `cats-effect`
  )
  .dependsOn(`console-sysinfo`)

lazy val `chatroom` = (project in file("chatroom"))
  .settings(
    libraryDependencies ++= fs2 ++ `cats-effect` ++ http4s ++ circe ++ pureconfig
  )

mainClass in (Compile, run) := Some("HttpSysInfo")

val CirceVersion = "0.12.3"
val Http4sVersion = "0.20.23"
val Specs2Version = "4.1.0"
val LogbackVersion = "1.2.3"
val FS2Version = "2.4.6"

val `cats-effect` = Seq(
  "org.typelevel" %% "cats-effect" % "2.2.0" withSources () withJavadoc (),
  "io.chrisdavenport" %% "cats-effect-time" % "0.1.2",
  "com.codecommit" %% "cats-effect-testing-scalatest" % "0.4.2" % Test
)
val `circe` = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % CirceVersion)

val `http4s` = Seq(
  "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
  "org.http4s" %% "http4s-circe" % Http4sVersion,
  "org.http4s" %% "http4s-dsl" % Http4sVersion,
  "io.circe" %% "circe-generic" % CirceVersion,
  "org.specs2" %% "specs2-core" % Specs2Version % "test",
  "ch.qos.logback" % "logback-classic" % LogbackVersion
)

val `pureconfig` = Seq(
  "com.github.pureconfig" %% "pureconfig" % "0.14.0",
  "com.github.pureconfig" %% "pureconfig-cats-effect" % "0.14.0"
)

val `fs2` = Seq(
  "co.fs2" %% "fs2-core" % FS2Version,
  "co.fs2" %% "fs2-io" % FS2Version,
  "co.fs2" %% "fs2-reactive-streams" % FS2Version,
  "co.fs2" %% "fs2-experimental" % FS2Version
)

scalacOptions in ThisBuild ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-encoding",
  "UTF-8",
  "-Xfatal-warnings",
  "-language:postfixOps",
  "-language:higherKinds",
  "-Ypartial-unification"
)
