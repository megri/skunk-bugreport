package com.megri.skunktest

import cats.effect._
import cats.syntax.all._

import org.flywaydb.core.Flyway

import natchez.Trace.Implicits.noop

import skunk._
import skunk.codec.all._
import skunk.implicits._

object BugReport extends IOApp {
  val session: Resource[IO, Session[IO]] =
    Session.single(
      host = "localhost",
      port = 5432,
      user = "postgres",
      database = "postgres"
    )

  val migrate = {
    val url = s"jdbc:postgresql://localhost:5432/postgres"
    for {
      flyway <- IO(Flyway.configure().dataSource(url, "postgres", null).load())
      _      <- IO(flyway.clean())
      _      <- IO(flyway.migrate())
    } yield ()
  }

  val badQuery =
    sql"""INSERT INTO foo ("bar_id") VALUES ($int8) RETURNING *""".query(int8 ~ int8)

  val runQuery = session.use(_.prepare(badQuery).use(_.unique(42)))

  def run(args: List[String]): IO[ExitCode] = {
    val result = for {
      _ <- migrate
      _ <- IO(println("Starting..."))
      r <- runQuery.attempt // hangs here
      _ <- IO(println("Done!"))
    } yield r

    // never happens
    result.flatMap {
      case Left(error) => IO(println(s"error: ${error.getMessage}")).as(ExitCode.Error)
      case Right(fooId ~ barId) => IO(println(s"success: data ($fooId, $barId)")).as(ExitCode.Success)
    }
  }
}
