lazy val root = (project in file(".")).
  settings(
    name := "brewcontrol",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.11.5",
    resolvers += Resolver.mavenLocal,

    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.9",
    libraryDependencies += "com.lihaoyi" %% "scalarx" % "0.2.7",
    libraryDependencies += "com.pi4j" % "pi4j-core" % "1.0-SNAPSHOT"
  )