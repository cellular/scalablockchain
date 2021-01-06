package core.util.catz

import cats.Monad

object MonadExtension {

  def foldOptionM[F[_], T](fa: F[Option[T]], fb: => F[T])(implicit m: Monad[F]): F[T] =
    Monad[F].flatMap(fa) {
    case Some(v) => m.pure(v)
    case _       => fb
  }

}
