import Dependencies._

ThisBuild / scalaVersion := "2.12.8"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.github.zella"
ThisBuild / organizationName := "zella"

lazy val root = (project in file("."))
  .settings(
    name := "rx-process2",
    libraryDependencies += "com.zaxxer" % "nuprocess" % "1.2.+",
    libraryDependencies += "io.reactivex.rxjava2" % "rxjava" % "2.2.+",
    libraryDependencies += "com.github.davidmoten" % "rxjava2-extras" % "0.1.+" % Test,
    libraryDependencies += scalaTest % Test
  )