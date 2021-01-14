package core

import zio.internal.Platform
import zio.{CancelableFuture, Runtime, ZIO}

trait ZIORuntime[F] {
  val environment: F

  private lazy val runtime: Runtime[F] = Runtime(environment, Platform.default)

  def runWithZIO[FF >: F, E <: Throwable, A](effect: ZIO[FF, E, A]): CancelableFuture[A] =
    runtime.unsafeRunToFuture[E, A](effect)

}
