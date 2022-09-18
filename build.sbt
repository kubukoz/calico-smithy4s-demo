ThisBuild / tlBaseVersion := "0.1"
ThisBuild / organization := "com.kubukoz"
ThisBuild / organizationName := "Jakub Kozłowski"
ThisBuild / startYear := Some(2022)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(tlGitHubDev("kubukoz", "Jakub Kozłowski"))

def crossPlugin(x: sbt.librarymanagement.ModuleID) = compilerPlugin(x.cross(CrossVersion.full))

val compilerPlugins = List(
  // crossPlugin("org.polyvariant" % "better-tostring" % "0.3.17")
)

ThisBuild / scalaVersion := "3.2.0"
ThisBuild / crossScalaVersions := Seq("3.2.0")

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / tlFatalWarnings := false
ThisBuild / tlFatalWarningsInCi := false

val commonSettings = Seq(
  scalacOptions -= "-Ykind-projector:underscores",
  libraryDependencies ++= compilerPlugins ++ Seq(
    "com.disneystreaming" %%% "weaver-cats" % "0.7.15" % Test,
    "com.disneystreaming" %%% "weaver-discipline" % "0.7.15" % Test,
    "com.disneystreaming" %%% "weaver-scalacheck" % "0.7.15" % Test,
  ),
  testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
)

lazy val core = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Full)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %%% "smithy4s-core" % smithy4sVersion.value
    ),
    smithy4sAllowedNamespaces := List("hello"),
  )
  .enablePlugins(Smithy4sCodegenPlugin)

lazy val front = crossProject(JSPlatform)
  .crossType(CrossType.Full)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "com.armanbilge" %%% "calico" % "0.1.1",
      "org.http4s" %%% "http4s-dom" % "0.2.3",
      "com.disneystreaming.smithy4s" %%% "smithy4s-http4s" % smithy4sVersion.value,
    ),
  )
  .jsSettings(
    Compile / fastOptJS / artifactPath := (ThisBuild / baseDirectory).value / "web" / "main.js",
    Compile / fullOptJS / artifactPath := (ThisBuild / baseDirectory).value / "web" / "main.js",
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.ESModule)),
  )
  .dependsOn(core)

lazy val server = crossProject(JVMPlatform)
  .crossType(CrossType.Full)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-server" % "0.23.16",
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value,
    ),
  )
  .dependsOn(core)

lazy val root = tlCrossRootProject
  .aggregate(core, front, server)
  .settings(
    Compile / doc / sources := Seq()
  )
