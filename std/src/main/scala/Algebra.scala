package psp
package std

import api._

/** TODO - how to abstract the entire notion of a Complement class and the
 *  always-the-same logic which accompanies it, without virtual classes?
 */
object Algebras {
  final case class Mapped[R, S](algebra: BooleanAlgebra[R], f: S => R, g: R => S) extends BooleanAlgebra[S] {
    def and(x: S, y: S): S = g(algebra.and(f(x), f(y)))
    def or(x: S, y: S): S  = g(algebra.or(f(x), f(y)))
    def not(x: S): S       = g(algebra.not(f(x)))
    def zero: S            = g(algebra.zero)
    def one: S             = g(algebra.one)
  }
  object Identity extends BooleanAlgebra[Boolean] {
    def and(x: Boolean, y: Boolean): Boolean = x && y
    def or(x: Boolean, y: Boolean): Boolean  = x || y
    def not(x: Boolean): Boolean             = !x
    def zero: Boolean                        = false
    def one: Boolean                         = true
  }
  object LabelAlgebra extends BooleanAlgebra[Label] {
    private def maybeParens(lhs: Label, op: String, rhs: Label): String =
      if (lhs.isSafe && rhs.isSafe) s"$lhs $op $rhs" else s"($lhs $op $rhs)"

    def and(x: Label, y: Label): Label = if (x.isZero || y.isZero) zero
                                         else if (x.isOne) y
                                         else if (y.isOne) x
                                         else Label(maybeParens(x, "&&", y))

    def or(x: Label, y: Label): Label  = if (x.isBool && !y.isBool) y
                                         else if (y.isBool && !x.isBool) x
                                         else Label(maybeParens(x, "||", y))

    def not(x: Label): Label           = Label( if (x.isSafe) s"!$x" else "!($x)" )
    def zero                           = Label.Zero
    def one                            = Label.One
  }

  final case class PredicateComplement[A](f: psp.std.Predicate[A]) extends psp.std.Predicate[A] with ForceShowDirect {
    def apply(x: A): Boolean = !f(x)
    def to_s = "!" + f
  }
  final class Predicate[A] extends BooleanAlgebra[psp.std.Predicate[A]] {
    private type R = psp.std.Predicate[A]

    /** TODO - one of of the benefits of having constant true and false is an
     *  opportunity to optimize expressions away entirely with no evaluation,
     *  if e.g. y is ConstantTrue in x(p) || y(p). Obviously this won't mix well
     *  with side effects. How enthusiastic can we be about punishing side effects
     *  before we kill the patient?
     */
    def and(x: R, y: R): R = p => x(p) && y(p)
    def or(x: R, y: R): R  = p => x(p) || y(p)
    def zero: R            = ConstantFalse
    def one: R             = ConstantTrue
    def not(f: R): R       = f match {
      case ConstantFalse          => ConstantTrue
      case ConstantTrue           => ConstantFalse
      case PredicateComplement(f) => f
      case _                      => PredicateComplement(f)
    }
  }

  final class InSetAlgebra[A] extends BooleanAlgebra[InSet[A]] {
    import InSet._

    def and(x: InSet[A], y: InSet[A]): InSet[A] = (x, y) match {
      case (Complement(xs), Complement(ys)) => not(Union[A](xs, ys))
      case (Complement(xs), ys)             => Diff[A](ys, xs)
      case (xs, Complement(ys))             => Diff[A](xs, ys)
      case _                                => Intersect[A](x, y)
    }
    def or(x: InSet[A], y: InSet[A]): InSet[A] = (x, y) match {
      case (Complement(xs), Complement(ys)) => not(Intersect(xs, ys))
      case (Complement(xs), ys)             => not(Diff(xs, ys))
      case (xs, Complement(ys))             => not(Diff(ys, xs))
      case _                                => Union(x, y)
    }
    def not(x: InSet[A]): InSet[A] = x match {
      case Zero           => one
      case One            => zero
      case Complement(xs) => xs            // unwrap
      case _              => Complement(x) // wrap
    }
    def zero: InSet[A] = Zero.castTo
    def one: InSet[A]  = One.castTo
  }
}
