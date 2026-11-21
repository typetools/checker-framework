// @below-java21-jdk-skip-test

// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Tests for step 2 of JLS 26 section 18.5.5 ("An initial bound set, B0, is generated from the
// declared bounds of P1, ..., Pn"), and for the deviation in step 5 ("resolution skips the step
// that attempts to produce an instantiation for an inference variable from its proper lower bounds
// or proper upper bounds; instead, any new instantiations are created by skipping directly to the
// step that introduces fresh type variables").
//
// When the matched type supplies no information, the annotations on the record's declared bounds
// are the only source of qualifiers for the component types.

import java.util.List;
import java.util.function.Supplier;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class DeclaredBounds {

  record UntaintedBox<T extends @Untainted Object>(T t) {}

  record TaintedBox<T extends @Tainted Object>(T t) {}

  /**
   * The matched type is Object, so the only bound of alpha, the inference variable for
   * UntaintedBox's type parameter (JLS 18.5.5), is the declared bound {@code @Untainted Object}.
   * Resolution introduces a fresh Y with that upper bound, and the upward projection is {@code
   * UntaintedBox<? extends @Untainted Object>}, so the component is @Untainted.
   */
  static void boundSuppliesQualifier(@Tainted Object o) {
    if (o instanceof UntaintedBox(var t)) {
      @Untainted Object tt = t;
    }
  }

  /** The same record, but now the matched type pins the type argument. */
  static void boundAndMatchedType(UntaintedBox<@Untainted String> b) {
    if (b instanceof UntaintedBox(var t)) {
      @Untainted String tt = t;
    }
  }

  /**
   * With a @Tainted declared bound and no information from the matched type, the component is top.
   */
  static void topBound(@Tainted Object o) {
    if (o instanceof TaintedBox(var t)) {
      @Tainted Object tt = t;
      // :: error: [assignment]
      @Untainted Object bad = t;
    }
  }

  /**
   * A raw matched type. A raw type is neither a parameterized type nor a parameterization of a
   * generic class, so steps 3 and 4 add nothing and alpha is resolved from the declared bound of
   * UntaintedBox's type parameter alone.
   */
  @SuppressWarnings("rawtypes")
  static void rawMatchedType(UntaintedBox b) {
    if (b instanceof UntaintedBox(var t)) {
      @Untainted Object tt = t;
    }
  }

  @SuppressWarnings("rawtypes")
  static void rawMatchedTypeTopBound(TaintedBox b) {
    if (b instanceof TaintedBox(var t)) {
      // :: error: [assignment]
      @Untainted Object bad = t;
    }
  }

  record Box<T>(T t) {}

  /**
   * Step 5 says resolution must NOT instantiate alpha from its proper lower bound; it must
   * introduce a fresh type variable instead. Here beta, the inference variable for the capture of
   * the wildcard, has the proper lower bound {@code @Untainted String} and no proper upper bound
   * other than Object, and alpha = beta, so alpha has the same bounds. If resolution wrongly
   * instantiated alpha to {@code @Untainted String}, the pattern type would be {@code
   * Box<@Untainted String>} and `t` would be @Untainted; the specified behavior gives {@code Box<?
   * super @Untainted String>}, whose component type is a top-qualified Object.
   */
  static void lowerBoundOnly(Box<? super @Untainted String> b) {
    if (b instanceof Box(var t)) {
      @Tainted Object tt = t;
      // :: error: [assignment]
      @Untainted Object bad = t;
    }
  }

  /**
   * The declared bound mentions the record's own type parameter and carries a qualifier on the
   * nested type argument.
   */
  record Sorted<T extends @Untainted Comparable<@Untainted T>>(T value) {}

  static void recursiveBound(@Tainted Object o) {
    if (o instanceof Sorted(var v)) {
      @Untainted Object vv = v;
    }
  }

  /**
   * The declared bounds of T and U mention each other, so alpha1 and alpha2 are resolved together
   * and each fresh type variable's upper bound mentions the other.
   */
  record Mut<T extends @Untainted List<U>, U extends @Untainted List<T>>(T t, U u) {}

  static void mutuallyDependentBounds(Mut<?, ?> o) {
    if (o instanceof Mut(var t, var u)) {
      @Untainted Object tt = t;
      @Untainted Object uu = u;
    }
  }

  record UntaintedSupplier<T extends @Untainted Object>(T t) implements Supplier<T> {
    public T get() {
      return t;
    }
  }

  /**
   * The matched type gives alpha = @Tainted String, which violates the declared bound
   * {@code @Untainted Object} of UntaintedSupplier's T.
   */
  static void matchedTypeViolatesBound(Supplier<@Tainted String> s) {
    // :: error: [type.arguments.not.inferred]
    if (s instanceof UntaintedSupplier(var t)) {
      @Tainted Object tt = t;
    }
  }
}
