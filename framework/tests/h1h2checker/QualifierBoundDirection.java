import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

/**
 * When a bound relates two inference variables, the qualifier bounds of one imply qualifier bounds
 * on the other, but only in the direction that the bound licenses. {@code
 * VariableBounds.addConstraintsFromComplementaryBounds} computes that direction.
 *
 * <p>Each test below depends on a qualifier written on a type parameter declaration, which is a
 * lower bound of the type parameter. Removing such a qualifier makes the test vacuous.
 */
public class QualifierBoundDirection {

  static <@H1S1 A> void take(A a1, A a2) {}

  static <B extends @H1S2 Object> B make(B b) {
    return b;
  }

  // The bound `B <: A` is a lower bound of A whose type is a use of B.  A has the qualifier lower
  // bound @H1S1, from the declared lower bound of the type parameter A.  From `@H1S1 <: A` nothing
  // follows about B, so B must not acquire @H1S1 as a qualifier lower bound.
  //
  // If it did, then B would be instantiated to the least upper bound of `@H1S2 String` and @H1S1,
  // namely `@H1Top String`.  That violates B's declared upper bound `@H1S2 Object`, so this method
  // would issue type.arguments.not.inferred.
  //
  // `take` has two formal parameters so that the inference variable for A has a proper lower bound
  // of its own, rather than only the use of B.  Otherwise A's instantiation and B's lower bound are
  // the same annotated type, and raising A to @H1Top raises B along with it, no matter which
  // qualifier bounds B has.
  void lowerBoundIsUseOfVariable(@H1S2 String s, @H1S2 String t) {
    take(make(s), t);
  }

  static <@H1S1 C> C makeC() {
    throw new AssertionError();
  }

  static <D> D lub(D d1, D d2) {
    return d1;
  }

  // The bound `C <: D` is an upper bound of C whose type is a use of D.  C has the qualifier lower
  // bound @H1S1, from the declared lower bound of the type parameter C.  From `@H1S1 <: C` and
  // `C <: D` it follows that `@H1S1 <: D`, so D must acquire @H1S1 as a qualifier lower bound.
  //
  // If it did not, then D would be instantiated to `@H1S2 String`, the type of `t`.  C, whose only
  // upper bound is D, would be instantiated to `@H1S2 String` as well, which does not satisfy C's
  // own qualifier lower bound @H1S1, so this method would issue type.arguments.not.inferred.
  void upperBoundIsUseOfVariable(@H1S2 String t) {
    @H1Top String x = lub(makeC(), t);
  }

  // As above, D is instantiated to the least upper bound of `@H1S2 String` and @H1S1, namely
  // `@H1Top String`.  There is no target type to refine that instantiation.
  void upperBoundIsUseOfVariableInferredType(@H1S2 String t) {
    var v = lub(makeC(), t);
    // :: error: [argument]
    needH1S2(v);
  }

  static void needH1S2(@H1S2 String s) {}
}
