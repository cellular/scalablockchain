package core

final case class Miner(block: Block, isMining: Boolean, canceledBlock: Boolean)
