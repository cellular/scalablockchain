package core.util

import core.Block

object SizeHelper {

  final val transactionSizeInBytes = 128
  final val blockHeaderSizeInBytes = 80
  final val blockMetaDataSizeInBytes = 12
  final val maxBlockSizeInBytes = 1120

  final val calculateTransactionCapacity: Int =
    (maxBlockSizeInBytes - blockMetaDataSizeInBytes - blockHeaderSizeInBytes) / transactionSizeInBytes

  def calculateBlockSize: Block => Int = block =>
    blockHeaderSizeInBytes + block.transactions.length * transactionSizeInBytes + blockMetaDataSizeInBytes

}
