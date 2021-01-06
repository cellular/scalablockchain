package org.tesseractblockchain

import java.time.{Clock, Instant, ZoneId, ZoneOffset}

import core.{Miner, ZIORuntime}
import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.inject.ApplicationLifecycle
import zio.{CancelableFuture, Fiber, Ref, ZIO}

@Singleton
private[tesseractblockchain] class Application @Inject()(
    applicationLifecycle: ApplicationLifecycle)(
    implicit clock: Clock
) extends ZIORuntime[Clock with DependencyManager] with Logging {

  override val environment: Clock with DependencyManager = new Clock with DependencyManager {
    override def getZone: ZoneId = ZoneId.of("UTC")
    override def withZone(zone: ZoneId): Clock = clock.withZone(zone)
    override def instant(): Instant = Instant.now(clock)
  }

  private def start(): CancelableFuture[Fiber.Runtime[Throwable, Ref[Miner]]] =
    liftZIO(ZIO.accessM[Clock with DependencyManager](_.runMining()))

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
