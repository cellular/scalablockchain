package org.tesseractblockchain.services

import core.{Block, BlockSize, Offset}
import javax.inject.Inject
import org.tesseractblockchain.BlockchainEnvironment
import zio.ZIO
import zio.interop.catz.monadErrorInstance

private[tesseractblockchain] class BlockService @Inject()() {

  def getBlockByHash(hex: String): ZIO[BlockchainEnvironment, Throwable, Block] =
    ZIO.accessM[BlockchainEnvironment](_.dependencyEnv.blockchain.getInstance(_.getBlockByHash(hex))).flatten

  def getRecentBlocks(size: BlockSize, offset: Option[Offset]): ZIO[BlockchainEnvironment, Throwable, List[Block]] =
    ZIO.accessM[BlockchainEnvironment](
      _.dependencyEnv.blockchain.getInstance(_.getLatestBlocks(size, offset.getOrElse(Offset.default)))
    ).flatten

  def getChildBlockOfHash(hex: String): ZIO[BlockchainEnvironment, Throwable, Block] =
    ZIO.accessM[BlockchainEnvironment] { dm =>
      for {
        block <- dm.dependencyEnv.blockchain.getInstance(_.getBlockByHash(hex)).flatten
        child <- dm.dependencyEnv.blockchain.getInstance(_.getChildOfBlock(block)).flatten
      } yield child
    }

}
