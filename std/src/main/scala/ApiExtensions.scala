package psp
package std
package ops

import api._
import Doc._
import StdZero._

trait DocStringOps extends Any {
  def self: String

  def <>(that: Doc): Doc  = doc <> that
  def <+>(that: Doc): Doc = doc <+> that
  def </>(that: Doc): Doc = doc </> that

  def doc: Doc           = Text(self)
  def backticked: Doc    = Text("`" + self + "`")
  def %(args: Doc*): Doc = FormatString(self) % (args: _*)
}

class Renderer(indentSize: Int) {
  var indentLevel: Int = 0
  def sp: String = " " * (indentLevel * indentSize)

  private def indentedBy[A](n: Int)(body: => A): A = {
    indentLevel += n
    try body finally indentLevel -= n
  }

  def apply(doc: Doc): String = doc match {
    case WordBreak                       => " "
    case LineBreak                       => "\n"
    case Group(doc)                      => apply(doc)
    case Line(alt)                       => alt
    case Nest(doc, Indent(n))            => indentedBy(n)(apply(doc))
    case Text(value)                     => value
    case Shown(value, z)                 => z show value
    case Cat(left, right)                => apply(left) ~ apply(right)
    case Format(FormatString(fmt), args) => fmt.format(args.seq map apply: _*)
  }
}
class DocOps(val lhs: Doc) extends AnyVal {
  
  def <>(rhs: String): Doc  = lhs <> rhs.doc
  def <+>(rhs: String): Doc = lhs <+> rhs.doc

  def <>(rhs: Doc): Doc   = Cat(lhs, rhs)
  def <+>(rhs: Doc): Doc  = lhs <> space <> rhs
  def </>(rhs: Doc): Doc  = lhs <> softline <> rhs
  def <@>(rhs: Doc): Doc  = lhs <> line <> rhs
  def <@@>(rhs: Doc): Doc = lhs <> linebreak <> rhs
  def <\>(rhs: Doc): Doc  = lhs <> softbreak <> rhs

  def render: String = new Renderer(2) apply lhs

  def to_str: String = lhs.render
  def length: Int    = to_str.length

  def last: Doc = lhs match {
    case Cat(left, right) => if (right.isEmpty) left.last else right.last
    case Group(x)         => x.last
    case _                => lhs
  }
  def first: Doc = lhs match {
    case Cat(left, right) => if (left.isEmpty) right.first else left.first
    case Group(x)         => x.first
    case _                => lhs
  }
  def isSpace: Boolean = lhs match {
    case WordBreak => true
    case Text(" ") => true
    case Group(x)  => x.isSpace
    case _         => false
  }
  def isEmpty: Boolean = lhs match {
    case Text("")  => true
    case Group(x)  => x.isEmpty
    case Cat(x, y) => x.isEmpty && y.isEmpty
    case _         => false
  }
  def endsSpace: Boolean = last.isSpace
  def endsLine: Boolean = last.isLine
  def isLine = lhs match {
    case Line(_) => true
    case _       => false
  }
  def isWhitespace: Boolean = isSpace || isLine
  def isNotWhitespace: Boolean = !isEmpty && !isWhitespace
  def indent(j: Int): Doc = Nest(linebreak <> lhs, Indent(j))
  def indent(): Doc       = indent(2)

  def surround(left: Doc, right: Doc): Doc       = left <> lhs <> right
  def surround(left: String, right: String): Doc = left.doc <> lhs <> right.doc

  def inSpaces: Doc       = surround(space, space)
  def inParens: Doc       = surround(lparen, rparen)
  def inBraces: Doc       = surround(lbrace, rbrace)
  def inBrackets: Doc     = surround(lbracket, rbracket)
  def inDoubleQuotes: Doc = surround(dquote, dquote)
  def grouped: Doc        = Group(lhs)
}

trait DocSeqCommonOps extends Any {
  def docs: DocSeq

  private def nonEmpties    = docs filterNot (_.isEmpty)
  private def isOnlyEmpties = nonEmpties.isEmpty

  // Empty if the seq is empty, otherwise apply the function.
  def opt(f: DocSeq => Doc): Doc = if (docs.isEmpty) empty else f(docs)
  def join(sep: Doc): Doc        = docs zreduce (_ <> sep <> _)
  def joinSpaced(sep: Doc): Doc  = docs zreduce (_ <+> sep <+> _)

  def inBracesBlock: Doc = if (isOnlyEmpties) lbrace <> space <> rbrace else joinLines.indent() surround (lbrace, line <> rbrace)
  def inBracesList: Doc  = joinComma surround (lbrace <> space, space <> rbrace)
  def inParens: Doc      = joinComma.inParens
  def joinChars: Doc     = this join empty
  def joinComma: Doc     = nonEmpties join comma <> space
  def joinDotted: Doc    = this join dot
  def joinLines: Doc     = this join line
  def joinParents: Doc   = this joinSpaced "with".doc
  def joinWords: Doc     = nonEmpties join space
  def optBrackets: Doc   = if (isOnlyEmpties) empty else joinComma.inBrackets
}

final class DocSeqOps(val docs: DocSeq) extends AnyVal with DocSeqCommonOps
final class ShowableSeqOps[A: Show](xs: Each[A]) extends AnyRef with DocSeqCommonOps {
  def docs: DocSeq                = xs map (_.doc)
}
final class IndexRangeOps(xs: IndexRange) {
  def *(n: Int): IndexRange = indexRange(xs.startInt * n, xs.endInt * n)
}

final class InMapOps[K, V](xs: InMap[K, V]) {
  def comap[K1](f: K1 => K): InMap[K1, V] = new InMap.Impl(xs.domain comap f, xs.lookup comap f)
  def partial: K ?=> V                    = newPartial(contains, xs.apply)
  def contains(key: K): Boolean           = xs domain key
  def ++(that: InMap[K, V]): InMap[K, V]  = new InMap.Impl(xs.domain union that.domain, xs.lookup orElse that.lookup)
}
final class ExMapOps[K, V](xs: ExMap[K, V]) {
  def ++(that: ExMap[K, V]): ExMap[K, V] = new ExMap.Impl(xs.domain union that.domain, xs.lookup orElse that.lookup)
}

final class InSetOps[A](xs: InSet[A]) {
  def comap[A1](f: A1 => A): InSet[A1]    = InSet(f andThen xs)
  def mapOnto[B](f: A => B): InMap[A, B]  = InMap(xs, f)
  def diff(that: InSet[A]): InSet[A]      = InSet.Diff(xs, that)
  def union(that: InSet[A]): InSet[A]     = InSet.Union(xs, that)
  def intersect(that: InSet[A]): InSet[A] = InSet.Intersect(xs, that)
  def complement: InSet[A] = xs match {
    case InSet.Complement(xs) => xs
    case _                             => InSet.Complement(xs)
  }

  def filter(p: Predicate[A]): InSet[A]    = this intersect inSet(p)
  def filterNot(p: Predicate[A]): InSet[A] = this filter !p
}
final class ExSetOps[A](xs: ExSet[A]) {
  private implicit def heq: HashEq[A] = xs.hashEq

  /** replace adds the element, ejecting any existing element which measures === to it.
   *  add adds the element only if no existing element is === to it.
   */

  def add(x: A): ExSet[A]                 = if (xs(x)) xs else xs union exSet(x)
  def canonicalize(x: A): A               = xs.findOr(_ === x, x)
  def diff(that: ExSet[A]): ExSet[A]      = ExSet.Diff(xs, that)
  def filter(p: Predicate[A]): ExSet[A]   = ExSet.Filtered(xs, p)
  def intersect(that: ExSet[A]): ExSet[A] = ExSet.Intersect(xs, that)
  def isSubsetOf(ys: InSet[A]): Boolean   = xs forall ys
  def mapOnto[B](f: A => B): ExMap[A, B]  = new ExMap.Impl(xs, Lookup total f)
  def replace(x: A): ExSet[A]             = if (xs(x)) without(x) add x else this add x
  def reverse: ExSet[A]                   = xs // XXX
  def union(that: ExSet[A]): ExSet[A]     = ExSet.Union(xs, that)
  def without(x: A): ExSet[A]             = xs diff exSet(x)

  def filterNot(p: Predicate[A]): ExSet[A] = this filter !p
}

trait HasPreciseSizeMethods extends Any {
  def size: Precise

  def longSize: Long      = size.value
  def intSize: Int        = longSize.safeInt
  def isZero: Boolean     = longSize == 0L
  def isPositive: Boolean = longSize > 0L
  def indices: IndexRange = indexRange(0, intSize)
  def nths: Direct[Nth]   = indices map (_.toNth)
  def lastIndex: Index    = Index(longSize - 1)  // effectively maps both undefined and zero to no index.
  def lastNth: Nth        = lastIndex.toNth

  def containsIndex(index: Index): Boolean            = indices contains index
  @inline def mapIndices[A](f: Index => A): Direct[A] = indices map f force
  @inline def foreachIndex(f: Index => Unit): Unit    = indices foreach f
  @inline def foreachNth(f: Nth => Unit): Unit        = indices foreach (i => f(i.toNth))
}

final class HasPreciseSizeOps(val x: HasPreciseSize) extends HasPreciseSizeMethods {
  def size: Precise = x.size
}

final class PreciseOps(val size: Precise) extends AnyRef with HasPreciseSizeMethods {
  def get: Long        = longSize
  def safeInt          = get.safeInt
  def toDouble: Double = get.toDouble

  def + (n: Int): Precise = (longSize + n).size
  def - (n: Int): Precise = (longSize - n).size
  def * (n: Int): Precise = (longSize * n).size
  def / (n: Int): Precise = (longSize / n).size
  def % (n: Int): Precise = (longSize % n).size

  def /+ (n: Int): Precise = (this / n) + ( if ((this % n).isZero) 0 else 1 )

  def + (n: Precise): Precise = (longSize + n.longSize).size
  def - (n: Precise): Precise = (longSize - n.longSize).size
  def * (n: Precise): Precise = (longSize * n.longSize).size
  def / (n: Precise): Precise = (longSize / n.longSize).size
  def % (n: Precise): Precise = (longSize % n.longSize).size

  def min(that: Precise): Precise = (longSize min that.longSize).size
  def max(that: Precise): Precise = (longSize max that.longSize).size
  def increment: Precise          = (longSize + 1L).size
  def decrement: Precise          = (longSize - 1L).size

  def timesConst[A](elem: A): Each[A]   = Each const elem take size
  def timesEval[A](body: => A): Each[A] = Each continually body take size

  def toIntRange                           = intRange(0, intSize)
  def padLeft(s: String, ch: Char): String = if (s.length >= longSize) s else (this - s.length timesConst ch mkString "") ~ s

  def leftFormatString  = if (size.isZero) "%s" else "%%-%ds" format intSize
  def rightFormatString = if (size.isZero) "%s" else "%%%ds" format intSize
  def leftFormat(arg: Doc): String  = leftFormatString format arg
  def rightFormat(arg: Doc): String = rightFormatString format arg

  def containsRange(range: IndexRange): Boolean = range.endInt <= intSize

  override def toString = s"$longSize"
}

final class BooleanAlgebraOps[A](val algebra: BooleanAlgebra[A]) extends AnyVal {
  def map[B](f: B => A, g: A => B): BooleanAlgebra[B] = new Algebras.Mapped[A, B](algebra, f, g)
}

final class InputStreamOps(val in: InputStream) extends AnyVal {
  def buffered: BufferedInputStream = in match {
    case in: BufferedInputStream => in
    case _                       => new BufferedInputStream(in)
  }
  def slurp(): Array[Byte]             = lowlevel.Streams slurp buffered
  def slurp(len: Precise): Array[Byte] = lowlevel.Streams.slurp(buffered, len)
}

final class StdOptOps[A](val x: Opt[A]) extends AnyVal {
  def fold[B](none: => B)(f: A => B): B = if (x.isEmpty) none else f(x.get)
  def |[A1 >: A](alt: => A1): A1        = if (x.isEmpty) alt else x.get
}

final class OrderBy[A]   { def apply[B](f: A => B)(implicit z: Order[B]): Order[A]   = Order[A]((x, y) => z.compare(f(x), f(y))) }
final class EqBy[A]      { def apply[B](f: A => B)(implicit z: Eq[B]): Eq[A]         = Eq[A]((x, y) => z.equiv(f(x), f(y)))      }
final class ShowBy[A]    { def apply[B](f: A => B)(implicit z: Show[B]): Show[A]     = Show[A](x => z show f(x))                 }
final class HashBy[A]    { def apply[B](f: A => B)(implicit z: Hash[B]): Hash[A]     = Hash[A](x => z hash f(x))                 }
final class HashEqBy[A]  { def apply[B](f: A => B)(implicit z: HashEq[B]): HashEq[A] = HashEq[A]((x, y) => z.equiv(f(x), f(y)), x => z hash f(x)) }
