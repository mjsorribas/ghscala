package ghscala

import scalaz.{One => _, Two => _, _}
import scalaz.concurrent.{Future, Task}
import Z._

final class ActionEOps[E, A](val self: ActionE[E, A]) extends AnyVal {

  def nel: ActionE[NonEmptyList[E], A] = self.leftMap(NonEmptyList.nel(_, Nil))

  def mapRequest(f: Config): ActionE[E, A] = Action[E, A](
    Z.mapSuspensionFreeC(self.run, new (RequestF ~> RequestF){
      def apply[A](a: RequestF[A]) = a.mapRequest(f)
    })
  )

  def task: Task[E \/ A] =
    Interpreter.task.empty.run(self)

  def task(conf: Config): Task[E \/ A] =
    Interpreter.task.apply(conf).run(self)

  def async: Future[E \/ A] =
    Interpreter.future.empty.run(self)

  def async(conf: Config): Future[E \/ A] =
    Interpreter.future.apply(conf).run(self)

  def withTime: Times[E \/ A] =
    Interpreter.times.empty.run(self)

  def withTime(conf: Config): Times[E \/ A] =
    Interpreter.times.apply(conf).run(self)

  def futureWithTime: Future[(List[Time], E \/ A)] =
    Interpreter.times.future.empty.run(self).run

  def futureWithTime(conf: Config): Future[(List[Time], E \/ A)] =
    Interpreter.times.future(conf).run(self).run

  def interpret: E \/ A =
    Interpreter.sequential.empty.run(self)

  def interpretWith(conf: Config): E \/ A =
    Interpreter.sequential.apply(conf).run(self)

  def interpretBy[F[_]: Monad](f: InterpreterF[F]): F[E \/ A] =
    Z.interpret(self.run)(f)

  def zipWithError[B, C, E1, E2](that: ActionE[E1, B])(f: (E \/ A, E1 \/ B) => E2 \/ C): ActionE[E2, C] =
    Action(Z.freeC(RequestF.two(self, that)(f)))

  def zip[B](that: ActionE[E, B])(implicit E: Semigroup[E]): ActionE[E, (A, B)] =
    zipWith(that)(Tuple2.apply)

  import syntax.apply._

  def zipWith[B, C](that: ActionE[E, B])(f: (A, B) => C)(implicit E: Semigroup[E]): ActionE[E, C] =
    zipWithError(that)((a, b) => (a.validation |@| b.validation)(f).disjunction)

}
