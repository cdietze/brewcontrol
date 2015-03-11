import spray.revolver.RevolverPlugin._

lazy val root = (project in file(".")).
  settings(Revolver.settings: _*).
  settings(
    name := "brewcontrol",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.11.5",
    resolvers += Resolver.mavenLocal,
    resolvers += Resolver.url("typesafe-ivy-repo", url("http://typesafe.artifactoryonline.com/typesafe/releases"))(Resolver.ivyStylePatterns),

    libraryDependencies ++= {
      val akkaVersion = "2.3.9"
      val sprayVersion = "1.3.2"

      Seq(
        "com.typesafe.akka" %% "akka-actor" % akkaVersion,
        "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
        "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",

        "org.scala-sbt" %% "io" % "0.13.7",

        "com.lihaoyi" %% "scalarx" % "0.2.7",

        "org.mongodb" %% "casbah" % "2.8.0",
        "com.github.nscala-time" %% "nscala-time" % "1.8.0",

        "com.lihaoyi" %% "utest" % "0.3.0" % "test",

        "io.spray" %% "spray-can" % sprayVersion,
        "io.spray" %% "spray-routing" % sprayVersion,
        "io.spray" %% "spray-testkit" % sprayVersion % "test",

        "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
        "ch.qos.logback" % "logback-classic" % "1.0.13"

      )
    },

    testFrameworks += new TestFramework("utest.runner.Framework"),

    fullClasspath in Revolver.reStart <<= fullClasspath in Test,
      mainClass in Revolver.reStart <<= mainClass in Test
)