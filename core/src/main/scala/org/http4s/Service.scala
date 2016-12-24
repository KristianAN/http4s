package org.http4s

import scalaz.{Kleisli, Monoid}
import scalaz.concurrent.Task
import scalaz.syntax.kleisli._
import scalaz.syntax.monoid._

object Service {
  /**
    * Lifts a total function to a `Service`. The function is expected to handle
    * all requests it is given.  If `f` is a `PartialFunction`, use `apply`
    * instead.
    */
  def lift[A, B](f: A => Task[B]): Service[A, B] = Kleisli.kleisli(f)

  /** Lifts a partial function to an `Service`.  Responds with the
    * zero of [B] for any request where `pf` is not defined.
    */
  def apply[A, B: Monoid](pf: PartialFunction[A, Task[B]]): Service[A, B] =
    lift(req => pf.applyOrElse(req, Function.const(Task.now(Monoid[B].zero))))

  /**
    * Lifts a Task into a [[Service]].
    *
    */
  def const[A, B](b: => Task[B]): Service[A, B] = b.liftKleisli

  /**
    *  Lifts a value into a [[Service]].
    *
    */
  def constVal[A, B](b: => B): Service[A, B] = Task.now(b).liftKleisli

  /** Allows Service chainig through a `scalaz.Monoid` instance. */
  def withFallback[A, B](fallback: Service[A, B])(service: Service[A, B])(implicit M: Monoid[Task[B]]): Service[A, B] =
    service |+| fallback

  /** A service that always returns the zero of B. */
  def empty[A, B: Monoid]: Service[A, B] =
    constVal(Monoid[B].zero)
}
