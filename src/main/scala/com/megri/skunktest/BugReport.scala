package com.megri.skunktest

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._

import cats._
import cats.data.Kleisli
import cats.effect._
import cats.implicits._

import natchez._

import org.flywaydb.core.Flyway

import skunk._
import skunk.codec.all._
import skunk.implicits._

object BugReport extends IOApp {

  def entryPoint[F[_]: Sync]: Resource[F, EntryPoint[F]] = {
    import natchez.log.Log
    import io.chrisdavenport.log4cats.Logger
    import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

    implicit val log: Logger[F] = Slf4jLogger.getLogger[F]
    Log.entryPoint[F]("foo").pure[Resource[F, *]]
  }

  def migrate[F[_]](implicit F: Sync[F]): F[Unit] = {
    val url = s"jdbc:postgresql://localhost:5432/postgres"
    for {
      flyway <- F.delay(Flyway.configure().dataSource(url, "postgres", null).load())
      _      <- F.delay(flyway.clean())
      _      <- F.delay(flyway.migrate())
    } yield ()
  }

  def session[F[_]: Concurrent: ContextShift: Trace]: Resource[F, Session[F]] =
    Session.single(
      host = "localhost",
      port = 5432,
      user = "postgres",
      database = "postgres",
      debug = true,
      readTimeout = 3.seconds,
      writeTimeout = 3.seconds
    )

  val badQuery: Query[Long, Long ~ Long] =
    sql"""INSERT INTO "foo" ("bar_id") VALUES ($int8) RETURNING *""".query(int8 ~ int8)

  def runQuery[F[_]: Concurrent: ContextShift: Trace] =
    session.use(_.prepare(badQuery).use(_.unique(42)))

  def runF[F[_]: Concurrent: ContextShift: Trace]: F[Either[Throwable, Long ~ Long]] =
    Trace[F].span("running query") {
      for {
        _ <- migrate
        r <- runQuery.attempt
      } yield r
    }

  def run(args: List[String]): IO[ExitCode] = {
    entryPoint[IO].use { ep =>
      ep.root("entrypoint").use { span =>
        runF[Kleisli[IO, Span[IO], *]].run(span).flatMap {
          case Left(err)   => IO(println(s"Failure: $err")).as(ExitCode.Error)
          case Right(data) => IO(println(s"Success: $data")).as(ExitCode.Success)
        }
      }
    }
  }
}
