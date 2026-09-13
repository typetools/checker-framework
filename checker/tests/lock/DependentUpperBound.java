// Test case for the last sentence of JLS 18.1.3: if the substituted upper bounds of a type
// parameter are all dependencies on other inference variables, then the bound
// "alpha <: Object" is also implied.
//
// Each declaration below has a type parameter whose only upper bound mentions an inference
// variable, so none of its substituted upper bounds is a proper type.  Every invocation of such a
// declaration exercises the "alpha <: Object" clause.

import java.util.Collections;
import java.util.List;

public class DependentUpperBound {

  static <T extends S, S> T dependentBound(T t) {
    return t;
  }

  static <T extends S, S extends Comparable<S>> T dependentBoundChain(T t) {
    return t;
  }

  static <T extends S, S> List<T> dependentBoundList(T t) {
    return Collections.singletonList(t);
  }

  static <T extends S, S> T noArgument() {
    throw new AssertionError();
  }

  static <T extends S, S, U extends S> T threeParameters(T t, U u) {
    return t;
  }

  static class Box<T extends S, S> {
    Box() {}
  }

  void use(String s) {
    Object o = dependentBound(s);
    String s2 = dependentBound(s);
    Object o2 = dependentBoundChain(s);
    String s3 = dependentBoundChain(s);
    List<String> l = dependentBoundList(s);
    String s4 = threeParameters(s, s);
  }

  void useWithoutArgument() {
    // The inference variable for T has no proper lower bound, because there is neither an
    // argument nor a target type.  Its only declared upper bound is the dependency on the
    // inference variable for S.
    noArgument();
    Object o = noArgument();
  }

  void useDiamond() {
    // The type parameters of a diamond are inference variables too, so Box's T also has only a
    // dependency as its upper bound.
    Box<?, ?> b = new Box<>();
    new Box<>();
  }
}
