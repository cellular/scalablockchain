package core.util.catz

import cats.Applicative
import zio.stm.{USTM, ZSTM}

object ZioApplicative {

  implicit val USTMApplicative: Applicative[USTM] = new Applicative[USTM] {
    override def pure[A](x: A): USTM[A] = ZSTM.succeed(x)
    override def ap[A, B](ff: USTM[A => B])(fa: USTM[A]): USTM[B] = ff.flatMap(f => fa.map(f))
  }

}
