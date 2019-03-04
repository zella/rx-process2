import Dependencies._

ThisBuild / scalaVersion := "2.12.8"
ThisBuild / version := "0.1.0-BETA"
ThisBuild / organization := "com.github.zella"
ThisBuild / organizationName := "zella"

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/zella/rx-process2"),
    "scm:git@github.com:zella/rx-process2.git"
  )
)

ThisBuild / developers := List(
  Developer(
    id    = "zella",
    name  = "Andrey Zelyaev",
    email = "drumirage@gmail.com",
    url   = url("https://github.com/zella")
  )
)

ThisBuild / description := "rx-java2 wrapper for NuProcess"
ThisBuild / licenses := List("MIT" -> new URL("https://opensource.org/licenses/MIT"))
ThisBuild / homepage := Some(url("https://github.com/zella/rx-process2"))

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true
//ThisBuild / useGpg := true 

lazy val root = (project in file("."))
  .settings(
    name := "rx-process2",
    libraryDependencies += "com.zaxxer" % "nuprocess" % "1.2.+",
    libraryDependencies += "io.reactivex.rxjava2" % "rxjava" % "2.2.+",
    libraryDependencies += "com.github.davidmoten" % "rxjava2-extras" % "0.1.+" % Test,
    libraryDependencies += scalaTest % Test
  )
