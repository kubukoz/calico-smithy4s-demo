ThisBuild / tlBaseVersion := "0.1"
ThisBuild / organization := "com.kubukoz"
ThisBuild / organizationName := "Jakub Kozłowski"
ThisBuild / startYear := Some(2022)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(tlGitHubDev("kubukoz", "Jakub Kozłowski"))

def crossPlugin(x: sbt.librarymanagement.ModuleID) = compilerPlugin(x.cross(CrossVersion.full))

val compilerPlugins = List(
  crossPlugin("org.polyvariant" % "better-tostring" % "0.3.17")
)

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / crossScalaVersions := Seq("2.13.8")

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / tlFatalWarnings := false
ThisBuild / tlFatalWarningsInCi := false

val commonSettings = Seq(
  libraryDependencies ++= compilerPlugins ++ Seq(
    "com.disneystreaming" %%% "weaver-cats" % "0.7.15" % Test,
    "com.disneystreaming" %%% "weaver-discipline" % "0.7.15" % Test,
    "com.disneystreaming" %%% "weaver-scalacheck" % "0.7.15" % Test,
  ),
  testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
)

lazy val app = crossProject(JSPlatform)
  .crossType(CrossType.Pure)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "com.raquo" %%% "laminar" % "0.13.1"
    ),
  )

lazy val root = tlCrossRootProject
  .aggregate(app)
  .settings(
    Compile / doc / sources := Seq()
  )
