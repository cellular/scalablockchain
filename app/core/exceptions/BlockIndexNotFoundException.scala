package core.exceptions

case object BlockIndexNotFoundException extends Exception {
  override def getMessage: String = s"block index not found"
}
