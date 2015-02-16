lazy val root = (project in file(".")).
  settings(
    name := "brewcontrol",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.11.5",
    resolvers += Resolver.mavenLocal,

    libraryDependencies += "com.pi4j" % "pi4j-core" % "1.0-SNAPSHOT",
    libraryDependencies += "com.lihaoyi" %% "scalarx" % "0.2.7"
  )