lazy val root = (project in file(".")).
  settings(
    name := "brewcontrol",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.11.5",
    resolvers += "Local Maven Repository" at Path.userHome.asFile.toURI.toURL + ".m2/repository",
    libraryDependencies += "framboos" % "framboos" % "0.0.1-SNAPSHOT"
  )