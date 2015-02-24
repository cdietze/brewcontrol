lazy val root = (project in file(".")).
  settings(
    name := "brewcontrol",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.11.5",
    resolvers += Resolver.mavenLocal,

    libraryDependencies ++= {
      val akkaVersion = "2.3.9"
      val sprayVersion = "1.3.2"

      Seq(
        "com.typesafe.akka" %% "akka-actor" % akkaVersion,
        "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",

        "com.lihaoyi" %% "scalarx" % "0.2.7",

        "org.mongodb" %% "casbah" % "2.8.0",
        "com.github.nscala-time" %% "nscala-time" % "1.8.0",

        "com.lihaoyi" %% "utest" % "0.3.0" % "test",

        "io.spray" %% "spray-can" % sprayVersion,
        "io.spray" %% "spray-routing" % sprayVersion,
        "io.spray" %% "spray-testkit" % sprayVersion % "test"
      )
    },

    testFrameworks += new TestFramework("utest.runner.Framework")
  )