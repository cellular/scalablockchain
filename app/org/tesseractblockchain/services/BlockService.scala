package org.tesseractblockchain.services

import core.{Block, BlockSize, Offset}
import org.tesseractblockchain.BlockchainEnvironment
import zio.ZIO
import zio.interop.catz.monadErrorInstance

private[tesseractblockchain] class BlockService {

  def getBlockByHash(hex: String): ZIO[BlockchainEnvironment, Throwable, Block] =
    ZIO.accessM[BlockchainEnvironment] { env =>
      for {
        blockchainRef <- env.dependencyEnv.blockchain
        block         <- blockchainRef.>>=(_.getBlockByHash(hex))
      } yield block
    }

  def getRecentBlocks(size: BlockSize, offset: Option[Offset]): ZIO[BlockchainEnvironment, Throwable, List[Block]] =
    ZIO.accessM[BlockchainEnvironment] { env =>
      for {
        blockchainRef <- env.dependencyEnv.blockchain
        blocks        <- blockchainRef.>>=(_.getLatestBlocks(size, offset.getOrElse(Offset.default)))
      } yield blocks
    }

  def getChildBlockOfHash(hex: String): ZIO[BlockchainEnvironment, Throwable, Block] =
    ZIO.accessM[BlockchainEnvironment] { env =>
      for {
        blockchainRef <- env.dependencyEnv.blockchain
        block         <- blockchainRef.>>=(_.getBlockByHash(hex))
        child         <- blockchainRef.>>=(_.getChildOfBlock(block))
      } yield child
    }

}
