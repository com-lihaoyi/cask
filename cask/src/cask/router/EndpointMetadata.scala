package cask.router

case class EndpointMetadata[T](decorators: Seq[Decorator[_, _, _, _]],
                               endpoint: Endpoint[_, _, _, _],
                               entryPoint: EntryPoint[T, _])
object EndpointMetadata{
  // `seqify` is used to statically check that the decorators applied to each
  // individual endpoint method line up, and each decorator's `OuterReturned`
  // correctly matches the enclosing decorator's `InnerReturned`. We don't bother
  // checking decorators defined as part of cask.Main or cask.Routes, since those
  // are both more dynamic (and hard to check) and also less often used and thus
  // less error prone
  def seqify1(d: Decorator[_, _, _, _]) = Seq(d)
  def seqify2[T1]
             (d1: Decorator[T1, _, _, _])
             (d2: Decorator[_, T1, _, _]) = Seq(d1, d2)
  def seqify3[T1, T2]
             (d1: Decorator[T1, _, _, _])
             (d2: Decorator[T2, T1, _, _])
             (d3: Decorator[_, T2, _, _]) = Seq(d1, d2, d3)
  def seqify4[T1, T2, T3]
             (d1: Decorator[T1, _, _, _])
             (d2: Decorator[T2, T1, _, _])
             (d3: Decorator[T3, T2, _, _])
             (d4: Decorator[_, T3, _, _]) = Seq(d1, d2, d3, d4)
  def seqify5[T1, T2, T3, T4]
             (d1: Decorator[T1, _, _, _])
             (d2: Decorator[T2, T1, _, _])
             (d3: Decorator[T3, T2, _, _])
             (d4: Decorator[T4, T3, _, _])
             (d5: Decorator[_, T4, _, _]) = Seq(d1, d2, d3, d4, d5)
  def seqify6[T1, T2, T3, T4, T5]
             (d1: Decorator[T1, _, _, _])
             (d2: Decorator[T2, T1, _, _])
             (d3: Decorator[T3, T2, _, _])
             (d4: Decorator[T4, T3, _, _])
             (d5: Decorator[T5, T4, _, _])
             (d6: Decorator[_, T5, _, _]) = Seq(d1, d2, d3, d4)
}
