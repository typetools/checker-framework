// @below-java21-jdk-skip-test

// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Tests for the last two bullets of step 3 of JLS 26 section 18.5.5:
//   * "If T is any other class or interface type, then T' is the same as T, and B1 is the same
//      as B0."
//   * "If T is a type variable or an intersection type, then for each upper bound of the type
//      variable or element of the intersection type, this step and step 4 are repeated
//      recursively.  All bounds produced in steps 3 and 4 are incorporated into a single bound
//      set."

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class TypeVarAndIntersection {

  record Box<T>(T t) {}

  record Producer<T>(T value) implements Supplier<T>, Cloneable {
    @Override
    public T get() {
      return value;
    }
  }

  /**
   * The matched type is Object, so the only bound on alpha, the inference variable for Box's type
   * parameter (JLS 18.5.5), comes from the declared bound of that type parameter. The pattern type
   * is {@code Box<?>}, and `s` is only known to be a top-qualified Object.
   */
  static void matchedAgainstObject(@Tainted Object o) {
    if (o instanceof Box(var s)) {
      @Tainted Object oo = s;
      // :: error: [assignment]
      @Untainted Object bad = s;
    }
  }

  /**
   * Even when the matched expression is @Untainted, Box's type argument is unknown, so the
   * component type is still top-qualified. (The @Untainted on `o` describes the Box, not its
   * contents.)
   */
  static void matchedAgainstUntaintedObject(@Untainted Object o) {
    if (o instanceof Box(var s)) {
      // :: error: [assignment]
      @Untainted Object bad = s;
    }
  }

  interface UntaintedStringSupplier extends Supplier<@Untainted String> {}

  record Holder<T>(T t) implements UntaintedStringSupplier {
    @Override
    public @Untainted String get() {
      return "";
    }
  }

  /**
   * The matched type is an interface type that is not parameterized, although its supertype {@code
   * Supplier<@Untainted String>} is. T' is the matched type, which is not a parameterization of a
   * generic class, so step 4 adds no constraint, and the only bound on alpha comes from the
   * declared bound of Holder's type parameter.
   */
  static void matchedAgainstNonGenericInterface(UntaintedStringSupplier u) {
    if (u instanceof Holder(var t)) {
      // :: error: [assignment]
      @Untainted Object bad = t;
    }
  }

  /**
   * The matched type is a type variable whose upper bound is a parameterization of Supplier, a
   * supertype of Producer.
   */
  static <Q extends Supplier<@Untainted String>> void typeVariable(Q q) {
    if (q instanceof Producer(var v)) {
      @Untainted String vv = v;
    }
  }

  static <Q extends Supplier<@Tainted String>> void typeVariableTainted(Q q) {
    if (q instanceof Producer(var v)) {
      // :: error: [assignment]
      @Untainted String vv = v;
    }
  }

  /**
   * The matched type is a type variable whose bound is an intersection type; only one element is
   * relevant.
   *
   * <p>(The bounds are written Cloneable-first because javac rejects the other ordering with "Q
   * cannot be safely cast to Producer<String>"; that is javac's intersection cast-safety check, not
   * a property of 18.5.5 inference.)
   */
  static <Q extends Cloneable & Supplier<@Untainted String>> void intersectionBound(Q q) {
    if (q instanceof Producer(var v)) {
      @Untainted String vv = v;
    }
  }

  /** A type variable whose bound is another type variable. */
  static <R extends Supplier<@Untainted String>, Q extends R> void nestedTypeVariable(Q q) {
    if (q instanceof Producer(var v)) {
      @Untainted String vv = v;
    }
  }

  record Both<A>(A a) implements Supplier<A>, Consumer<A> {
    @Override
    public A get() {
      return a;
    }

    @Override
    public void accept(A x) {}
  }

  /**
   * Both elements of the intersection constrain alpha, consistently. Per step 3, the bounds from
   * both recursive applications go into a single bound set.
   */
  static <Q extends Supplier<@Untainted String> & Consumer<@Untainted String>>
      void consistentIntersection(Q q) {
    if (q instanceof Both(var a)) {
      @Untainted String aa = a;
    }
  }

  /**
   * This is legal Java (the two interfaces have different erasures), but the two elements of the
   * intersection put contradictory bounds on alpha: alpha = @Untainted String from one and alpha
   * = @Tainted String from the other. javac's inference sees alpha = String either way and
   * succeeds. The Checker Framework's qualified inference fails, and the pattern gets its Java type
   * with default qualifiers, so `a` is @Tainted.
   */
  static <Q extends Supplier<@Untainted String> & Consumer<@Tainted String>>
      void inconsistentIntersection(Q q) {
    // :: error: [type.arguments.not.inferred]
    if (q instanceof Both(var a)) {
      // :: error: [assignment]
      @Untainted String aa = a;
    }
  }
}
