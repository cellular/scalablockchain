package core.exceptions

final case class BlockNotFoundException(hex: String) extends Exception {
  override def getMessage: String = s"Block not found: $hex"
}
