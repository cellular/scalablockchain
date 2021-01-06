package core.util.jsonextensions

case object JsonInternalServerError extends Throwable {
  override def getMessage: String = "org.tesseractblockchain.errors.internalServerError"
}
