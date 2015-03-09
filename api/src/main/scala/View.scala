package psp
package api

import ApiAliases._

trait AnyView[+A] extends Any with Each[A] {
  type MapTo[+X] <: AnyView[X]
}

trait SetView[A] extends Any with AnyView[A] with ExSet[A] {
}

/** Contiguous operations share the property that the result is always
 *  a (possibly empty) uninterrupted subsequence of the elements of the
 *  target collection.
 */
trait ContiguousViewOps[+A] extends Any with AnyView[A] {
  type Contiguous[+X] <: View[X]

  // TODO:
  //
  // def init: Contiguous[A]
  // def tail: Contiguous[A]
  // def span(p: ToBool[A]): ContiguousSplit[A]
  // def splitAt(index: Index): ContiguousSplit[A]
  def drop(n: Precise): Contiguous[A]
  def dropRight(n: Precise): Contiguous[A]
  def dropWhile(p: ToBool[A]): Contiguous[A]
  def take(n: Precise): Contiguous[A]
  def takeRight(n: Precise): Contiguous[A]
  def takeWhile(p: ToBool[A]): Contiguous[A]
}

trait NonContiguousViewOps[+A] extends Any with AnyView[A] {
  def ++[A1 >: A](that: View[A1]): View[A1]
  def collect[B](pf: A ?=> B): MapTo[B]
  def map[B](f: A => B): MapTo[B]
  def flatMap[B](f: A => Each[B]): MapTo[B]
  def withFilter(p: ToBool[A]): MapTo[A]
}

trait View[+A] extends Any with ContiguousViewOps[A] with NonContiguousViewOps[A] {
  type MapTo[+X] <: View[X]

  def viewOps: Direct[String]
}

trait ContiguousView[+A] extends Any with View[A] {
  type Contiguous[+X] <: ContiguousView[X]
}

trait InvariantView[A] extends Any with View[A] {
  def join(that: InvariantView[A]): InvariantView[A]
  def partition(p: ToBool[A]): SplitView[A]
  def span(p: ToBool[A]): SplitView[A]
  def splitAt(index: Index): SplitView[A]
}

trait InMapView[-K, +V] extends Any with AnyView[V] with InMap[K, V] {
  type CoMapTo[-X] <: InMapView[X, V]
  type MapTo[+X] <: InMapView[K, X]

  def comap[K1](f: K1 => K): CoMapTo[K1]
  def filter(p: ToBool[V]): MapTo[V]
  def map[V1](f: V => V1): MapTo[V1]
}
