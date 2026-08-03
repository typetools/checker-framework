// Regression test for the last sentence of JLS 18.1.3: if the substituted upper bounds of a type
// parameter are all dependencies on other inference variables, then the bound
// "alpha <: Object" is also implied.
//
// In each declaration below the dependencies among the type parameters are cyclic, so no proper
// upper bound propagates to the first type parameter.  Without the JLS 18.1.3 clause, the
// inference variable for that type parameter has no proper upper bound at all, and resolution
// throws a NullPointerException ("upperBound is null" in
// InferenceFactory.createFreshTypeVariable).
//
// NOTE: this test also requires Resolution.resolveWithCapture to use every upper bound of the
// variable rather than only VariableBounds.upperBounds(), which drops upper bounds that are uses
// of inference variables.  JLS 18.4 says the fresh variable's upper bound is
// glb(U1 theta, ..., Uk theta) over *all* upper bounds.

import java.util.List;
import java.util.Map;

public class DependentUpperBoundCycle {

  static <T extends S, S extends List<T>> T listCycle() {
    throw new AssertionError();
  }

  static <T extends S, S extends Comparable<T>> T comparableCycle() {
    throw new AssertionError();
  }

  static <K extends V, V extends Map<K, V>> K mapCycle() {
    throw new AssertionError();
  }

  static <T extends S, S extends Iterable<T>> List<T> listReturnCycle() {
    throw new AssertionError();
  }

  void use() {
    listCycle();
    comparableCycle();
    mapCycle();
    Object o = listCycle();
    List<?> l = listReturnCycle();
  }
}
