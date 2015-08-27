
lazy val jvm = (project in file("jvm"))
  .settings(Revolver.settings: _*)
  .settings(Revolver.enableDebugging(port = 5050, suspend = false))
  .settings(
    name := "brewcontrol",
    version := "0.2-SNAPSHOT",
    scalaVersion := "2.11.7",
    scalacOptions += "-target:jvm-1.8",
    libraryDependencies ++= {
      val akkaVersion = "2.3.9"
      val sprayVersion = "1.3.2"

      Seq(
        "com.lihaoyi" %% "scalatags" % "0.4.6",
        "com.lihaoyi" %% "upickle" % "0.3.4",
        "com.lihaoyi" %% "scalarx" % "0.2.8",

        "com.typesafe.akka" %% "akka-actor" % akkaVersion,
        "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
        "com.typesafe.akka" %% "akka-agent" % akkaVersion,

        "org.scala-sbt" %% "io" % "0.13.7",

        "com.typesafe.slick" %% "slick" % "3.0.1",
        "org.xerial" % "sqlite-jdbc" % "3.7.2",

        "io.spray" %% "spray-can" % sprayVersion,
        "io.spray" %% "spray-routing" % sprayVersion,

        "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
        "ch.qos.logback" % "logback-classic" % "1.0.13",

        "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
        "io.spray" %% "spray-testkit" % sprayVersion % "test",
        "org.scalatest" %% "scalatest" % "2.2.4" % "test"
      )
    },
    fullClasspath in Revolver.reStart <<= fullClasspath in Test,
    mainClass in Revolver.reStart <<= mainClass in Test
  )