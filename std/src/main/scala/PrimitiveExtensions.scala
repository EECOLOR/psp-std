package psp
package std
package ops

import java.{ lang => jl }
import api._, StdShow._
import lowlevel._

final object UnitOps {
  @inline def thenReturn[A](value: A): A = value
}

final class AnyOps[A](val x: A) extends AnyVal {
  // Short decoded class name.
  def shortClass: String   = x.getClass.shortName
  def shortPackage: String = x.getClass.shortPackage

  // "Maybe we can enforce good programming practice with annoyingly long method names."
  def castTo[U] : U   = x.asInstanceOf[U]
  def toRef: AnyRef   = castTo[AnyRef]
  def isNull: Boolean = toRef eq null

  def reflect[B](m: jMethod)(args: Any*): B = m.invoke(x, args.map(_.castTo[AnyRef]): _*).castTo[B]

  // The famed forward pipe.
  @inline def doto(f: A => Unit): A               = sideEffect(f(x))
  @inline def isOr(p: A => Boolean)(alt: => A): A = if (p(x)) x else alt
  @inline def sideEffect(body: Unit): A           = x
  @inline def |>[B](f: A => B): B                 = f(x)

  // Calling eq on Anys.
  def id_==(y: Any): Boolean = toRef eq y.toRef
  def id_## : Int            = identityHashCode(x)

  def optionally[B](pf: A ?=> B): Option[B] = if (pf isDefinedAt x) Some(pf(x)) else None
  def requiring(p: Predicate[A]): Option[A] = if (p(x)) Some(x) else None
  def matchOr[B](alt: => B)(pf: A ?=> B): B = if (pf isDefinedAt x) pf(x) else alt
  def try_s(implicit z: TryShow[A]): String = z show x
  def any_s: String = "" + x
}

final class AnyRefOps[A <: AnyRef](val x: A) extends AnyVal {
  @inline def foldNull[B](zero: => B)(f: A => B): B = if (x eq null) zero else f(x)
  @inline def doto(f: A => Unit): x.type     = sideEffect(f(x))
  @inline def sideEffect(body: Unit): x.type = x

  def debug(implicit z: Show[A]): x.type = sideEffect(echoErr(z show x))
  def this_s(implicit z: Show[A]): Doc   = "[".asis <> x.shortClass.asis <> "]".asis <+> x.doc
  def debug_s(implicit z: Show[A]): Doc  = x.shortClass.asis <+> "id=".asis <> x.id_##.doc <+> x.doc
}

final class CharOps(val ch: Char) extends AnyVal {
  def to_s: String                = ch.toString
  def prev: Char                  = (ch - 1).toChar
  def next: Char                  = (ch + 1).toChar
  def isAlphabetic                = jl.Character isAlphabetic ch
  def isDigit                     = jl.Character isDigit ch
  def toUpper                     = jl.Character toUpperCase ch
  def toLower                     = jl.Character toLowerCase ch
  def isUpper                     = jl.Character isUpperCase ch
  def isLower                     = jl.Character isLowerCase ch
  def isWhitespace                = jl.Character isWhitespace ch
  def isControl                   = jl.Character isISOControl ch
  def to(end: Char): Direct[Char] = ch.toInt to end.toInt map (_.toChar)
}

final class BooleanOps(val self: Boolean) extends AnyVal {
  def toInt: Int   = if (self) 1 else 0
  def toLong: Long = if (self) 1L else 0L
}

final class IntOps(val self: Int) extends AnyVal {
  private type This = Int

  def offset: Offset = Offset(self)
  def size: IntSize  = Precise(self)

  /** Make a 64-bit long by concatenating two 32-bit Ints.
   *  Retrieve the original Ints with left32 and right32.
   */
  @inline def join64(that: Int): Long = (self.toLong << 32) | that.toLong

  def abs: This               = scala.math.abs(self)
  def max(that: This): This   = scala.math.max(self, that)
  def min(that: This): This   = scala.math.min(self, that)
  def signum: This            = scala.math.signum(self)
  def lowerBound(n: Int): Int = max(n)
  def zeroPlus: Int           = lowerBound(0)

  def isOr(p: Int => Boolean)(alt: => Int): Int = if (p(self)) self else alt
  def until(end: Int): ExclusiveIntRange = intRange(self, end)
  def to(end: Int): ExclusiveIntRange    = nthRange(self, end)

  def u: UInt                         = UInt(self)
  def binary: String                  = jl.Integer.toBinaryString(self)
  def hex: String                     = jl.Integer.toHexString(self)
  def octal: String                   = jl.Integer.toOctalString(self)
}

final class LongOps(val self: Long) extends AnyVal {
  private type This = Long

  def nth: Nth       = Nth(self)
  def index: Index   = Index(self)
  def size: LongSize = Precise(self)

  def abs: This                 = scala.math.abs(self)
  def max(that: This): This     = scala.math.max(self, that)
  def min(that: This): This     = scala.math.min(self, that)
  def signum: This              = scala.math.signum(self)
  def lowerBound(n: Long): Long = max(n)
  def zeroPlus: Long            = lowerBound(0)
  def isNonZero: Boolean        = self != 0L

  def left32: Int  = (self >>> 32).toInt
  def right32: Int = self.toInt
  def <<(index: IndexLike): Long = self << index.toIndex.indexValue

  /** Safe in the senses that it won't silently truncate values, and
   *  will translate MaxLong to MaxInt instead of -1.
   */
  def safeToInt: Int = self match {
    case MaxLong => MaxInt
    case MinLong => MinInt
    case _       =>
      assert(self <= (MaxInt: Long), s"$self < $MaxInt")
      self.toInt
  }

  def toUnsignedInt: UInt = UInt(self)
  def binary: String      = jl.Long.toBinaryString(self)
  def hex: String         = jl.Long.toHexString(self)
  def octal: String       = jl.Long.toOctalString(self)
}