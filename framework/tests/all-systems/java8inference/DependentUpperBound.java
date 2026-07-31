// Test case for the last sentence of JLS 18.1.3: if the substituted upper bounds of a type
// parameter are all dependencies on other inference variables, then the bound
// "alpha <: Object" is also implied.

public class DependentUpperBound {

  static <T extends S, S> T dependentBound(T t) {
    return t;
  }

  static <T extends S, S extends Comparable<S>> T dependentBoundChain(T t) {
    return t;
  }

  static <T extends S, S> java.util.List<T> dependentBoundList(T t) {
    return java.util.Collections.singletonList(t);
  }

  static <T extends S, S> T noArgument() {
    throw new AssertionError();
  }

  void use(String s) {
    Object o = dependentBound(s);
    String s2 = dependentBound(s);
    Object o2 = dependentBoundChain(s);
    String s3 = dependentBoundChain(s);
    java.util.List<String> l = dependentBoundList(s);
  }

  void useWithoutArgumentOrTargetType() {
    // The inference variable for T has no proper lower bound, because there is neither an
    // argument nor a target type.  It also has no proper upper bound, because its only upper
    // bound is the dependency on the inference variable for S.  Without the "alpha <: Object"
    // bound, resolution has nothing to instantiate T to.
    noArgument();
  }
}
