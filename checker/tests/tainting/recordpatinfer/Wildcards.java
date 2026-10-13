// @below-java21-jdk-skip-test

// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Tests for step 3 of JLS 26 section 18.5.5: when T is a parameterized type, capture conversion is
// applied and the captured type variables Z1..Zk are replaced by fresh inference variables
// beta1..betak whose bounds come from the captures' bounds.  Also tests step 6, the upward
// projection of R<A1,...,An> with respect to the fresh type variables introduced by resolution.

package recordpatinfer;

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class Wildcards {

  record Box<T>(T t) {}

  /**
   * T is {@code Box<? extends @Untainted String>}. Capture gives {@code Box<Z>} with Z <:
   * {@code @Untainted String}; beta replaces Z with the same upper bound; alpha = beta; resolution
   * yields a fresh Y with upper bound {@code @Untainted String}; the upward projection of {@code
   * Box<Y>} is {@code Box<? extends @Untainted String>}. So `s` is {@code @Untainted String}.
   */
  static void extendsWildcardUntainted(Box<? extends @Untainted String> b) {
    if (b instanceof Box(var s)) {
      @Untainted String ss = s;
    }
  }

  /** Same, but the wildcard's upper bound is @Tainted. */
  static void extendsWildcardTainted(Box<? extends @Tainted String> b) {
    if (b instanceof Box(var s)) {
      // :: error: (assignment)
      @Untainted String ss = s;
    }
  }

  /**
   * A `super` wildcard: beta has the lower bound {@code @Untainted String} and (since no proper
   * upper bound otherwise exists) the upper bound Object. The component type is therefore only
   * known to be an Object with the default (top) qualifier.
   */
  static void superWildcard(Box<? super @Untainted String> b) {
    if (b instanceof Box(var s)) {
      @Tainted Object o = s;
      // :: error: (assignment)
      @Untainted Object bad = s;
    }
  }

  /** An unbounded wildcard: beta <: Object only. */
  static void unboundedWildcard(Box<?> b) {
    if (b instanceof Box(var s)) {
      @Tainted Object o = s;
      // :: error: (assignment)
      @Untainted Object bad = s;
    }
  }

  /**
   * A qualifier written on the wildcard itself, rather than on its bound. This exercises whether
   * the qualifier of the wildcard survives capture conversion and the substitution [Z := beta].
   */
  static void qualifierOnWildcard(Box<@Untainted ? extends @Untainted String> b) {
    if (b instanceof Box(var s)) {
      @Untainted String ss = s;
    }
  }

  /** A nested wildcard: the inner pattern's T is itself a wildcard-parameterized type. */
  static void nestedWildcard(Box<? extends @Untainted Box<? extends @Untainted String>> bbs) {
    if (bbs instanceof Box(Box(var s))) {
      @Untainted String ss = s;
    }
  }

  static void nestedWildcardInnerTainted(
      Box<? extends @Untainted Box<? extends @Tainted String>> bbs) {
    if (bbs instanceof Box(Box(var s))) {
      // :: error: (assignment)
      @Untainted String ss = s;
    }
  }

  /** The bound is a parameterized type, so the projection has to keep the nested type argument. */
  record Holder<T>(java.util.List<T> list) {}

  static void listComponent(Holder<? extends @Untainted String> h) {
    if (h instanceof Holder(var list)) {
      @Untainted String first = list.get(0);
    }
  }

  static void listComponentTainted(Holder<? extends @Tainted String> h) {
    if (h instanceof Holder(var list)) {
      // :: error: (assignment)
      @Untainted String first = list.get(0);
    }
  }
}
