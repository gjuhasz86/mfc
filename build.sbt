lazy val nodeDir = "/usr/bin"

// PROJECT SETTINGS //
lazy val root = (project in file("."))
  .dependsOn(backend)
  .settings(
    name := "mforecast",
    version := "1.0",
    scalaVersion := "2.12.1",
    target := baseDirectory.value / "target" / name.value,
    mainClass in(Compile, run) := Some("com.gjuhasz.mforecast.server.WebServer")
  )

lazy val backend = (project in file("mforecast-backend"))
  .dependsOn(shared)
  .settings(
    target := baseDirectory.value / ".." / "target" / name.value
  )


lazy val shared = (project in file("mforecast-shared"))
  .settings(
    resourceDirectory in Compile := baseDirectory.value / "resources",
    target in Compile := baseDirectory.value / ".." / "target" / name.value,
    target in Test := baseDirectory.value / ".." / "target" / name.value,
    artifactPath in(Compile, fastOptJS) :=
      baseDirectory.value / ".." / "dist" / s"${ name.value }.js",
    artifactPath in(Compile, fullOptJS) :=
      baseDirectory.value / ".." / "dist" / s"${ name.value }.js"
  )

lazy val web = (project in file("mforecast-web"))
  .settings(
    target := baseDirectory.value / ".." / "target" / name.value
  )


// TASKS //

lazy val distShared = taskKey[Unit]("Dists the shared project")
distShared := {
  (fastOptJS in Compile in shared).value
  IO.copyFile((baseDirectory in shared).value / "resources" / "package.json", (baseDirectory in root).value / "dist" / "package.json")
}

lazy val purge = taskKey[Unit]("Deletes all generated files")
purge := {
  delete(streams.value.log)(
    baseDir.value / "target",
    baseDir.value / "project" / "target",
    baseDir.value / "project" / "project",
    baseDir.value / "dist",
    (baseDirectory in web).value / "js",
    (baseDirectory in web).value / "node_modules"
  )
}

lazy val npmInstall = taskKey[Unit]("Execute the npm build command")
npmInstall := {
  distShared.value
  Process("npm install", Some(file("mforecast-web")), "PATH" -> (nodeDir + ":" + sys.env("PATH")))
    .run(procLogger(streams.value.log))
    .exitValue()
  Process("npm uninstall mforecast-shared", Some(file("mforecast-web")), "PATH" -> (nodeDir + ":" + sys.env("PATH")))
    .run(procLogger(streams.value.log))
    .exitValue()
  Process("npm install mforecast-shared@file:../dist", Some(file("mforecast-web")), "PATH" -> (nodeDir + ":" + sys.env("PATH")))
    .run(procLogger(streams.value.log))
    .exitValue()
}

lazy val tsBuild = taskKey[Unit]("Execute the typescrupt build command")
tsBuild := {
  npmInstall.value
  Process("npm run tsc", Some(file("mforecast-web")), "PATH" -> (nodeDir + ":" + sys.env("PATH")))
    .run(procLogger(streams.value.log))
    .exitValue()
}

lazy val gulpServe = taskKey[Unit]("Execute the gulp serve command")
gulpServe := {
  tsBuild.value
  Process("npm run startgulp", Some(file("mforecast-web")), "PATH" -> (nodeDir + ":" + sys.env("PATH")))
    .run(procLogger(streams.value.log))
    .exitValue()
}

lazy val showPath = taskKey[Unit]("shows PATH")
showPath := {
  Seq("sh", "echo $PATH")
    .run(procLogger(streams.value.log))
    .exitValue()
}


// HELPERS //

lazy val baseDir = baseDirectory in root

def procLogger(log: Logger) =
  new sbt.ProcessLogger {
    override def info(s: => String): Unit = log.info(s)
    override def error(s: => String): Unit = log.info(s)
    override def buffer[T](f: => T): T = f
  }


def delete(log: Logger)(files: File*): Unit =
  files.foreach { file =>
    log.info(s"Deleting [$file]")
    IO.delete(file)
  }
