package core.exceptions

import core.Block

final case class BlockChildNotFoundException(child: Block) extends Exception {
  override def getMessage: String = s"Block not found: $child"
}
