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

val Scala3 = "3.4.2"

ThisBuild / scalaVersion := Scala3
ThisBuild / crossScalaVersions := Seq(Scala3)

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / tlFatalWarnings := false

val commonSettings = Seq(
  scalacOptions -= "-Ykind-projector:underscores",
  libraryDependencies ++= compilerPlugins ++ Seq(
    "com.disneystreaming" %%% "weaver-cats" % "0.8.4" % Test,
    "com.disneystreaming" %%% "weaver-scalacheck" % "0.8.4" % Test,
  ),
)

lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %%% "smithy4s-core" % smithy4sVersion.value
    ),
    smithy4sAllowedNamespaces := List("hello"),
    Compile / smithy4sInputDirs := List(
      (ThisBuild / baseDirectory).value / "core" / "shared" / "src" / "main" / "smithy"
    ),
  )
  .enablePlugins(Smithy4sCodegenPlugin)

val frontLink = taskKey[File]("Build the frontend. If RELEASE is set, uses fullOptJS.")
val yarnBuild = taskKey[File]("Build the web app. Returns the dist directory")

lazy val front = crossProject(JSPlatform)
  .crossType(CrossType.Full)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "com.armanbilge" %%% "calico" % "0.2.2",
      "org.http4s" %%% "http4s-dom" % "0.2.11",
      "com.disneystreaming.smithy4s" %%% "smithy4s-http4s" % smithy4sVersion.value,
    ),
  )
  .jsSettings(
    frontLink := {
      def ifRelease[A](ifTrue: A, ifFalse: A): A =
        if (sys.env.contains("RELEASE"))
          ifTrue
        else
          ifFalse

      val in =
        ifRelease(
          ifTrue = Compile / fullOptJS,
          ifFalse = Compile / fastOptJS,
        ).value

      val inPath =
        ifRelease(
          ifTrue = Compile / fullOptJS / artifactPath,
          ifFalse = Compile / fastOptJS / artifactPath,
        ).value

      val outPath = (ThisBuild / baseDirectory).value / "web" / "main.js"

      IO.copyFile(inPath, outPath)
      outPath
    },
    yarnBuild := {
      // Using hash, because the file is copied every time for code simplicity
      FileFunction.cached(streams.value.cacheStoreFactory, FileInfo.hash) { (_, _) =>
        import sys.process._
        Process(
          List(
            "yarn",
            "--cwd",
            ((ThisBuild / baseDirectory).value / "web").toString,
            "build",
          )
        ).!

        Set.empty[File]
      }(Set(frontLink.value))

      (ThisBuild / baseDirectory).value / "web" / "dist"
    },
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.ESModule)),
  )
  .dependsOn(core)

lazy val cli = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s" %%% "http4s-ember-client" % "0.23.27",
      "com.disneystreaming.smithy4s" %%% "smithy4s-http4s" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %%% "smithy4s-decline" % smithy4sVersion.value,
      "com.monovore" %%% "decline-effect" % "2.4.1",
    )
  )
  .nativeSettings(
    libraryDependencies ++= Seq(
      "com.armanbilge" %%% "epollcat" % "0.1.4"
    )
  )
  .jsSettings(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule)),
  )
  .dependsOn(core)
  .enablePlugins(JavaAppPackaging)

lazy val server = crossProject(JVMPlatform)
  .crossType(CrossType.Full)
  .settings(
    commonSettings,
    fork := true,
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-server" % "0.23.27",
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value,
    ),
    Compile / resourceGenerators += Def.task {
      import sys.process._

      val frontendDir = (front.js / yarnBuild).value
      val targetDir = (Compile / resourceManaged).value / "frontend"

      IO.delete(targetDir)
      IO.copyDirectory(frontendDir, targetDir)

      Path.allSubpaths(targetDir).map(_._1).toList
    },
  )
  .dependsOn(core)
  .enablePlugins(JavaAppPackaging)
  .settings(
    dockerBaseImage := "openjdk:11-jre-slim"
  )
  .enablePlugins(DockerPlugin)

lazy val root = tlCrossRootProject
  .aggregate(core, front, cli, server)
  .settings(
    Compile / doc / sources := Seq()
  )
