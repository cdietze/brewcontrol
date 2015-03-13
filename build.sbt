
val app = crossProject.settings(
  unmanagedSourceDirectories in Compile +=
    baseDirectory.value / "shared" / "main" / "scala",
  libraryDependencies ++= Seq(
    "com.lihaoyi" %%% "scalatags" % "0.4.6",
    "com.lihaoyi" %%% "upickle" % "0.2.8",
    "com.lihaoyi" %%% "scalarx" % "0.2.8"
  ),
  scalaVersion := "2.11.5").
  jsSettings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.8.0"
    )
  ).
  jvmSettings(Revolver.settings: _*).
  jvmSettings(
    libraryDependencies ++= {
      val akkaVersion = "2.3.9"
      val sprayVersion = "1.3.2"

      Seq(
        "com.typesafe.akka" %% "akka-actor" % akkaVersion,
        "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
        "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",

        "org.scala-sbt" %% "io" % "0.13.7",

        "org.mongodb" %% "casbah" % "2.8.0",
        "com.github.nscala-time" %% "nscala-time" % "1.8.0",

        "com.lihaoyi" %% "utest" % "0.3.0" % "test",

        "io.spray" %% "spray-can" % sprayVersion,
        "io.spray" %% "spray-routing" % sprayVersion,
        "io.spray" %% "spray-testkit" % sprayVersion % "test",

        "org.scalatest" %% "scalatest" % "2.2.1" % "test",

        "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
        "ch.qos.logback" % "logback-classic" % "1.0.13"
      )
    },
    testFrameworks += new TestFramework("utest.runner.Framework"),
    fullClasspath in Revolver.reStart <<= fullClasspath in Test,
    mainClass in Revolver.reStart <<= mainClass in Test
  )

lazy val appJS = app.js
lazy val appJVM = app.jvm.settings(
  (resources in Compile) += (fastOptJS in(appJS, Compile)).value.data,
  (resources in Compile) += file((fastOptJS in(appJS, Compile)).value.data.absolutePath + ".map")
)
