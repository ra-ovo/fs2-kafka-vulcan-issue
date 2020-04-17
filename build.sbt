ThisBuild / scalaVersion     := "2.12.11"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "fs2-kafka vulcan issue",
    resolvers += "confluent-release" at "https://packages.confluent.io/maven/",
    resolvers += Resolver.bintrayRepo("ovotech", "maven"),
    scalacOptions += "-Ypartial-unification",
    fork in run := true, 
    libraryDependencies ++= libs ++ testLibs
  )

lazy val libs = Seq(
  "co.fs2" %% "fs2-core" % "2.2.1",
  "com.github.fd4s" %% "vulcan" % "1.0.1",
  "com.github.fd4s" %% "fs2-kafka" % "1.0.0",
  "com.github.fd4s" %% "fs2-kafka-vulcan" % "1.0.0",
)

lazy val testLibs = Seq(
  "org.scalatest" %% "scalatest" % "3.1.1"
) map ( _ % Test)

