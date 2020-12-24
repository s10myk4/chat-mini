import sbt.Keys._
import sbt.{Compile, Test}

object BaseSettings {

  def apply() = Seq(
    organization := "com.s10myk4",
    scalaVersion := "2.13.3",
    version := "1.0",
    scalacOptions in Compile ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-encoding",
      "UTF-8",
      "-Xlog-reflective-calls",
      "-Xlint"),
    fork in run := false,
    parallelExecution in Test := false
  )

}
