package core

import core.exceptions.BlockNotFoundException
import zio.Task

final case class BlockCache(value: List[(String, Block)]) extends AnyVal

object BlockCache {

  implicit class RichBlockCache(blockCache: BlockCache) {
    def add(hex: String, block: Block): List[(String, Block)] = (hex, block) :: blockCache.value

    def get(hex: String): Task[Block] =
      blockCache.value.find({ case (key, _) => key == hex }) match {
        case Some((_, block)) => Task.succeed(block)
        case None => Task.fail(BlockNotFoundException(hex))
      }
  }

}
