lazy val root = (project in file(".")).
  settings(
    name := "brewcontrol",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.11.5",
    resolvers += Resolver.mavenLocal,

    libraryDependencies ++= {
      Seq(
        "com.typesafe.akka" %% "akka-actor" % "2.3.9",
        "com.lihaoyi" %% "scalarx" % "0.2.7",
        "org.mongodb" %% "casbah" % "2.8.0",
        "com.github.nscala-time" %% "nscala-time" % "1.8.0",

        "com.lihaoyi" %% "utest" % "0.3.0" % "test",
        "com.typesafe.akka" %% "akka-testkit" % "2.3.9" % "test"
      )
    },

    testFrameworks += new TestFramework("utest.runner.Framework")
  )