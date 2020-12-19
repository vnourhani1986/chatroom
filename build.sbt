import Dependencies._

name := "chatroom-service"

version in ThisBuild := "2.2.0"

scalaVersion in ThisBuild := "2.12.8"

lazy val core = (project in file("./modules/core"))
  .settings(
    libraryDependencies ++= fs2
  )

lazy val `plugins` = (project in file("./modules/plugins"))
  .settings(
    libraryDependencies ++= fs2
  )
  .dependsOn(core)

lazy val `websocket-sysinfo` = (project in file("./modules/websocket-sysinfo"))
  .settings(
    libraryDependencies ++= fs2 ++ `cats-effect` ++ http4s
  )
  .dependsOn(`plugins`, core)

lazy val `rest-sysinfo` = (project in file("./modules/rest-sysinfo"))
  .settings(
    libraryDependencies ++= fs2 ++ `cats-effect` ++ http4s
  )
  .dependsOn(`plugins`, core)

lazy val `cli-sysinfo` = (project in file("./modules/cli-sysinfo"))
  .settings(
    libraryDependencies ++= fs2 ++ `cats-effect`
  )
  .dependsOn(`plugins`, core)

lazy val chatroom = (project in file("chatroom"))
  .settings(
    libraryDependencies ++= fs2 ++ `cats-effect` ++ http4s ++ circe
  ).dependsOn(core, plugins, `websocket-sysinfo`, `rest-sysinfo`)

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

