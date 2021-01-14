package tests

import ch.qos.logback.classic.LoggerContext
import org.mockito.scalatest.IdiomaticMockito
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import org.slf4j.LoggerFactory
import zio.{Runtime, Task, ZEnv}

import scala.concurrent.ExecutionContext
import scala.util.Try

class TestSpec extends PlaySpec
  with IdiomaticMockito
  with ScalaFutures
  with BeforeAndAfterEach
  with BeforeAndAfterAll {

  protected val futurePatienceTimeout: Int = 5

  protected val futurePatienceInterval: Int = 15

  implicit val defaultRuntime: Runtime[ZEnv] = Runtime.default

  implicit val defaultExecutionContext: ExecutionContext = ExecutionContext.Implicits.global

  implicit val defaultPatience: PatienceConfig = PatienceConfig(
    timeout = Span(futurePatienceTimeout, Seconds),
    interval = Span(futurePatienceInterval, Millis)
  )

  override def beforeAll(): Unit = {
    try super.beforeAll()
    finally {
      Try(LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]).map { ctx =>
        ctx.stop()
        org.slf4j.bridge.SLF4JBridgeHandler.uninstall()
      }
    }
  }

  def testZIO[T, U](task: Task[T])(f: Either[Throwable, T] => U): U =
    defaultRuntime.unsafeRun(task.either.map(f))

}
