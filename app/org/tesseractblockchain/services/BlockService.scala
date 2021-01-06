package org.tesseractblockchain.services

import core.{Block, BlockSize, Offset}
import javax.inject.Inject
import org.tesseractblockchain.DependencyManager
import zio.ZIO
import zio.interop.catz.monadErrorInstance

private[tesseractblockchain] class BlockService @Inject()() {

  def getBlockByHash(hex: String): ZIO[DependencyManager, Throwable, Block] =
    ZIO.accessM[DependencyManager](_.blockchain.getInstance(_.getBlockByHash(hex))).flatten

  def getRecentBlocks(size: BlockSize, offset: Option[Offset]): ZIO[DependencyManager, Throwable, List[Block]] =
    ZIO.accessM[DependencyManager](_.blockchain.getInstance(_.getLatestBlocks(size, offset.getOrElse(Offset.default)))).flatten

  def getChildBlockOfHash(hex: String): ZIO[DependencyManager, Throwable, Block] =
    ZIO.accessM[DependencyManager] { dm =>
      for {
        block <- dm.blockchain.getInstance(_.getBlockByHash(hex)).flatten
        child <- dm.blockchain.getInstance(_.getChildOfBlock(block)).flatten
      } yield child
    }

}
