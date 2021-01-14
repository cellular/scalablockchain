package org.tesseractblockchain

import java.time.{Clock, Instant, ZoneId, ZoneOffset}

import cats.implicits._
import core.interops.persistence.services.PersistenceService
import core.{Miner, ZIORuntime}
import javax.inject.{Inject, Singleton}
import play.api.Logging
import zio._
import zio.interop.catz._

@Singleton
private[tesseractblockchain] class Application @Inject()(
    clock: Clock,
    persistenceService: PersistenceService,
    dependencyManager: DependencyManager
) extends ZIORuntime[BlockchainEnvironment] with Logging {

  override val environment: BlockchainEnvironment = new BlockchainEnvironment {
    override val clockEnv: Clock = new Clock {
      override def getZone: ZoneId = ZoneId.of("UTC")
      override def withZone(zone: ZoneId): Clock = clock.withZone(zone)
      override def instant(): Instant = Instant.now(clock)
    }
    override val persistenceEnv: PersistenceService = persistenceService
    override val dependencyEnv: DependencyManager = dependencyManager
  }

  def program: ZIO[BlockchainEnvironment, Throwable, Fiber.Runtime[Throwable, Ref[Miner]]] =
    Task {
      logger.info(
        s"""
           |Application started...
           |Zulu time: ${clock.instant().atOffset(ZoneOffset.UTC)}
           |""".stripMargin)
    } *> ZIO.accessM[BlockchainEnvironment](_.dependencyEnv.runMining())

  def run(): Unit = runWithZIOAsync(program) {
    case Exit.Success(fiber) => fiber.join.bimap(t => onErrorFailureLog(t) *> program, _ => program)
    case Exit.Failure(cause) => onErrorFailureLog(cause.failures: _*) *> program
  }

  def onErrorFailureLog(throwable: Throwable*): Task[Unit] =
    throwable.traverse(t => Task(logger.error(s"Application abruptly ends -- reason(s): ${t.getMessage}"))) *>
    Task(logger.error(s"Zulu time: ${clock.instant().atOffset(ZoneOffset.UTC)}"))

  run()
}
