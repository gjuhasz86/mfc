name := "mforecast-backend"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.12.1"

scalaSource in Compile := baseDirectory.value / "src"
scalaSource in Test := baseDirectory.value / "test-src"

libraryDependencies += "org.scala-lang.modules" % "scala-java8-compat_2.12" % "0.8.0"
libraryDependencies += "org.scalatest" % "scalatest_2.12" % "3.0.1"
libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"

libraryDependencies += "com.typesafe.akka" %% "akka-http-core" % "10.0.3"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.3"
libraryDependencies += "com.typesafe.akka" %% "akka-http-testkit" % "10.0.3"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.3"
libraryDependencies += "com.typesafe.akka" %% "akka-http-jackson" % "10.0.3"
libraryDependencies += "com.typesafe.akka" %% "akka-http-xml" % "10.0.3"

libraryDependencies += "com.chuusai" %% "shapeless" % "2.3.2"

libraryDependencies += "io.circe" %%% "circe-core" % "0.7.0"
libraryDependencies += "io.circe" %%% "circe-generic" % "0.7.0"
libraryDependencies += "io.circe" %%% "circe-parser" % "0.7.0"
libraryDependencies += "de.heikoseeberger" %% "akka-http-circe" % "1.15.0"
