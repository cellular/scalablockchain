package core.persistence.errors

final case class FileWritingBlockThrowable(t: Throwable) extends Throwable {
  override def getMessage: String = s"write block throwable - reason: ${t.getMessage}"
}
