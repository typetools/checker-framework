// Test that incorporation (JLS 18.3) runs until every inference variable has reached a fixed
// point, not just until the last inference variable is unchanged.
//
// Incorporation repeatedly applies instantiations to bounds and reduces the constraints implied by
// complementary bounds.  The mutually F-bounded type parameters below require more than one such
// round:  a constraint is added to the bounds of the first inference variable, N, only while the
// bounds of a later inference variable are being processed, and the last inference variable, X, is
// unconstrained by the invocation and so never changes.
//
// If incorporation stops as soon as the last inference variable is unchanged, then the constraint
// on N is dropped without ever being reduced.  That constraint is the one that compares N's lower
// bound, @Tainted MyNode, against N's declared upper bound, @Untainted Node<...>, so dropping the
// constraint makes type argument inference report success even though the inferred type argument
// violates the qualifier bound.
//
// This test is sensitive to the exact shape of the type hierarchy below.  Removing the type
// parameter X, or removing the type parameter G together with class Tag, makes the constraint on N
// be derived during the first round of incorporation, in which case the test no longer
// distinguishes an early exit from a fixed point.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class IncorporationFixedPoint {

  abstract static class Node<N extends Node<N, E, G>, E extends Edge<N, E, G>, G extends Tag<G>> {}

  abstract static class Edge<N extends Node<N, E, G>, E extends Edge<N, E, G>, G extends Tag<G>> {}

  abstract static class Tag<G extends Tag<G>> {}

  static final class MyNode extends Node<MyNode, MyEdge, MyTag> {}

  static final class MyEdge extends Edge<MyNode, MyEdge, MyTag> {}

  static final class MyTag extends Tag<MyTag> {}

  // The same hierarchy, but with @Untainted type arguments, so that the inferred type argument for
  // N is within its declared bound.  These classes exercise the other outcome of the constraint
  // that incorporation derives only after reaching a fixed point:  the constraint is derived and
  // then reduces to true.
  abstract static class UNode<
      N extends UNode<N, E, G>, E extends UEdge<N, E, G>, G extends Tag<G>> {}

  abstract static class UEdge<
      N extends UNode<N, E, G>, E extends UEdge<N, E, G>, G extends Tag<G>> {}

  static final class MyUNode extends UNode<@Untainted MyUNode, MyUEdge, MyTag> {}

  static final class MyUEdge extends UEdge<@Untainted MyUNode, MyUEdge, MyTag> {}

  static class Holder<
      N extends @Untainted Node<N, E, G>,
      E extends Edge<N, E, G>,
      G extends Tag<G>,
      // X is mentioned by no formal parameter, so no constraint on X is ever created and X's
      // bounds never change during incorporation.
      X extends Tag<X>> {
    Holder(E e) {}
  }

  // The @Untainted bound on N is not consistent with the defaulted (that is, @Tainted) bounds of
  // Node and Edge, so the declaration of m issues type.argument errors that are beside the point
  // here.
  @SuppressWarnings("type.argument") // the invocation of m, not the declaration of m, is under test
  static <
          N extends @Untainted Node<N, E, G>,
          E extends Edge<N, E, G>,
          G extends Tag<G>,
          X extends Tag<X>>
      void m(E e) {}

  @SuppressWarnings("type.argument") // as for m, above
  static <
          N extends @Untainted UNode<N, E, G>,
          E extends UEdge<N, E, G>,
          G extends Tag<G>,
          X extends Tag<X>>
      void mUntainted(E e) {}

  // Without a fixed point, inference succeeds and no error at all is issued here.
  void diamondConstructor(@Tainted MyEdge e) {
    // N is inferred as @Tainted MyNode, which is not within its declared bound,
    // @Untainted Node<...>.
    // :: error: [type.arguments.not.inferred]
    new Holder<>(e);
  }

  // Without a fixed point, inference succeeds and the violation is caught later, by the type
  // argument bound check, which issues a type.argument error rather than this one.
  void genericMethod(@Tainted MyEdge e) {
    // N is inferred as @Tainted MyNode, which is not within its declared bound,
    // @Untainted Node<...>.
    // :: error: [type.arguments.not.inferred]
    m(e);
  }

  // N is inferred as @Untainted MyUNode, which is within its declared bound, so the constraint
  // that incorporation derives reduces to true and inference succeeds.
  void noFalsePositive(@Untainted MyUEdge e) {
    mUntainted(e);
  }
}
