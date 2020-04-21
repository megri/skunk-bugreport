ThisBuild / scalaVersion := "2.13.1"

lazy val core = project.in(file("."))
  .settings(
    libraryDependencies ++= Seq(
      "org.flywaydb" % "flyway-core" % "6.3.2",
      "org.postgresql" % "postgresql" % "42.2.12",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "org.tpolecat" %% "skunk-core" % "0.0.8",
    ),
    run / fork := true,
  )
