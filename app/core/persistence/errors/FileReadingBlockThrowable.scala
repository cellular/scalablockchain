package core.persistence.errors

import java.io.File

final case class FileReadingBlockThrowable(f: File) extends Throwable {
  override def getMessage: String = s"write block throwable - file: ${f.getName}"
}
