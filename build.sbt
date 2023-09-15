ThisBuild / scalaVersion     := "2.13.9"
ThisBuild / organization     := "io.github.euphoric-hardware"
ThisBuild / homepage         := Some(url("https://github.com/euphoric-hardware/chisel-recipes"))
ThisBuild / licenses         := List("BSD 3-Clause" -> url("https://spdx.org/licenses/BSD-3-Clause.html"))
ThisBuild / developers       := List(
  Developer("vighneshiyer", "Vighnesh Iyer", "vighnesh.iyer@berkeley.edu", url("https://vighneshiyer.com/")),
  Developer("bdngo", "Bryan Ngo", "bryanngo@berkeley.edu", url("https://bdngo.github.io/"))
)
ThisBuild / versionScheme    := Some("semver-spec")
ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"

val chiselVersion = "3.6.0"

lazy val root = (project in file("."))
  .settings(
    name := "chisel-recipes",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3" % chiselVersion,
      "edu.berkeley.cs" %% "chiseltest" % "0.6.0" % "test",
      "com.lihaoyi" %% "sourcecode" % "0.3.0"
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit"
    ),
    sonatypeRepository := "https://s01.oss.sonatype.org/service/local",
    addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % chiselVersion cross CrossVersion.full),
  )

