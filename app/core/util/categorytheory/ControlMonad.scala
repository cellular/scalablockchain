package core.util.categorytheory

import cats.Monad

object ControlMonad {
  def foldOptionM[F[_], T](onFailure: => F[T], onSuccess: F[Option[T]])(implicit M: Monad[F]): F[T] =
    Monad[F].flatMap(onSuccess) {
      case Some(v) => M.pure(v)
      case _       => onFailure
    }
}
