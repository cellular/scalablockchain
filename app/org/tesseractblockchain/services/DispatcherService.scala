package org.tesseractblockchain.services

import cats.Applicative
import cats.effect._
import javax.inject.Inject
import org.tesseractblockchain.DependencyManager
import org.tesseractblockchain.errors.SearchingHashNotFoundError
import org.tesseractblockchain.models.SearchForHashResponse
import zio.{Task, ZIO}
import zio.interop.catz._

private[tesseractblockchain] class DispatcherService @Inject()() {

  def searchForHash(hex: String): ZIO[DependencyManager, Throwable, SearchForHashResponse] =
    ZIO.accessM[DependencyManager] { dm =>
      Applicative[Task].map2(
        dm.blockchain.getInstance(_.getBlockByHash(hex)).flatten,
        dm.blockchain.getInstance(_.getTransactionByHash(hex)).flatten
      )((block, transaction) => SearchForHashResponse(block, transaction))
    }.mapError(_ => SearchingHashNotFoundError(hex))

}
