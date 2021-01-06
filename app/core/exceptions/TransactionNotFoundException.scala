package core.exceptions

final case class TransactionNotFoundException(hex: String) extends Exception {
  override def getMessage: String = s"Transaction not found: $hex"
}
