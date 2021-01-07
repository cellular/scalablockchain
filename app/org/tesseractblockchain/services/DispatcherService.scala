package org.tesseractblockchain.services

import cats.Applicative
import cats.effect._
import javax.inject.Inject
import org.tesseractblockchain.BlockchainEnvironment
import org.tesseractblockchain.errors.SearchingHashNotFoundError
import org.tesseractblockchain.models.SearchForHashResponse
import zio.interop.catz._
import zio.{Task, ZIO}

private[tesseractblockchain] class DispatcherService @Inject()() {

  def searchForHash(hex: String): ZIO[BlockchainEnvironment, Throwable, SearchForHashResponse] =
    ZIO.accessM[BlockchainEnvironment] { env =>
      Applicative[Task].map2(
        env.dependencyEnv.blockchain.getInstance(_.getBlockByHash(hex)).flatten,
        env.dependencyEnv.blockchain.getInstance(_.getTransactionByHash(hex)).flatten
      )((block, transaction) => SearchForHashResponse(block, transaction))
    }.mapError(_ => SearchingHashNotFoundError(hex))

}
