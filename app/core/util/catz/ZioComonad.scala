package core.util.catz

import cats.Comonad
import core.util.runtime.DefaultRuntime
import zio.Task

object ZioComonad {

  implicit val comonadTask: Comonad[Task] = new Comonad[Task] {
    override def extract[A](x: Task[A]): A = DefaultRuntime.runtime.unsafeRun(x)
    override def coflatMap[A, B](fa: Task[A])(f: Task[A] => B): Task[B] = Task(f(fa))
    override def map[A, B](fa: Task[A])(f: A => B): Task[B] = fa.map(f)
  }

}
