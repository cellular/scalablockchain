package org.tesseractblockchain.errors

private[tesseractblockchain] final case class SearchingHashNotFoundError(hex: String) extends Throwable {
  override def getMessage = s"Searched hash $hex could not found."
}
