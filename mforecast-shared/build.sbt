name := "mforecast-shared"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.12.1"

enablePlugins(ScalaJSPlugin)

publish := {}
publishLocal := {}

scalaSource in Compile := baseDirectory.value / "src"
scalaSource in Test := baseDirectory.value / "src"

libraryDependencies += "fr.hmil" %%% "roshttp" % "2.0.1"
libraryDependencies += "org.scala-js" %%% "scalajs-java-time" % "0.2.0"
libraryDependencies += "io.circe" %%% "circe-core" % "0.7.0"
libraryDependencies += "io.circe" %%% "circe-generic" % "0.7.0"
libraryDependencies += "io.circe" %%% "circe-parser" % "0.7.0"
libraryDependencies += "de.surfice" %%% "scalajs-rxjs" % "0.0.2"