package core.util.catz

import cats.Monad

object ControlMonad {

  def foldOptionM[F[_], T](onFailure: => F[T], onSuccess: F[Option[T]])(implicit m: Monad[F]): F[T] =
    Monad[F].flatMap(onSuccess) {
      case Some(v) => m.pure(v)
      case _       => onFailure
    }

}
