package psp
package core
package api

trait ViewEnvironment[A, Repr, CC[X]] {
  def unknownView(tc: ForeachableType[A, Repr, CC]): AtomicView
  def linearView(tc: LinearableType[A, Repr, CC]): LinearView
  def indexedView(tc: IndexableType[A, Repr, CC]): IndexedView

  type View[+X] <: api.View[X]
  type AtomicView <: View[A] with Foreach[A]
  type LinearView <: AtomicView with Linear[A]
  type IndexedView <: AtomicView with Indexed[A]
}
