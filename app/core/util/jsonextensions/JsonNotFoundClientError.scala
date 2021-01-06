package core.util.jsonextensions

case object JsonNotFoundClientError extends Throwable {
  override def getMessage: String = "org.tesseractblockchain.errors.notFoundClientError"
}
