import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.Keys.{scalacOptions, _}
import sbt.librarymanagement.CrossVersion
import sbt.{Compile, Test}

object BaseSettings {

  def apply() = Seq(
    organization := "com.s10myk4",
    scalaVersion := Versions.scala213,
    version := "1.0.0-SNAPSHOT",
    scalacOptions in Compile ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-encoding",
      "UTF-8",
      "-Xlog-reflective-calls",
      "-Xlint"
    ),
    //Seq(
    //  "-feature",
    //  "-deprecation",
    //  "-unchecked",
    //  "-encoding",
    //  "UTF-8",
    //  "-language:_",
    //  "-target:jvm-1.8"
    //)
    fork in run := true,
    parallelExecution in Test := false,
    scalafmtOnCompile in Compile := true,
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 => "-Ymacro-annotations" :: Nil
        case _ => "-Ypartial-unification" :: Nil
      }
    }
  )

}
