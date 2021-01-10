package org.tesseractblockchain

import java.time.{Clock, Instant, ZoneId, ZoneOffset}

import core.interops.persistence.services.PersistenceService
import core.{Miner, ZIORuntime}
import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.inject.ApplicationLifecycle
import zio.{CancelableFuture, Fiber, Ref, ZIO}

@Singleton
private[tesseractblockchain] class Application @Inject()(
    applicationLifecycle: ApplicationLifecycle)(
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

  private def start(): CancelableFuture[Fiber.Runtime[Throwable, Ref[Miner]]] =
    liftZIO(ZIO.accessM[BlockchainEnvironment](_.dependencyEnv.runMining()))

  logger.info(
    s"""
       |Application started...
       |Zulu time: ${clock.instant().atOffset(ZoneOffset.UTC)}
       |""".stripMargin)

  applicationLifecycle.addStopHook { () =>
    logger.error(
      s"""
         |Application abruptly ends!
         |Zulu time: ${clock.instant().atOffset(ZoneOffset.UTC)}
         |""".stripMargin)
    start()
  }

  start()
}
