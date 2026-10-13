// @below-java21-jdk-skip-test

// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Tests for step 4 of JLS 26 section 18.5.5: if T' is a parameterization of a generic class G and
// some supertype R' of R<alpha1,...,alphan> is also a parameterization of G, then the constraint
// formula <T' = R'> is reduced.  Here the matched type is a supertype of the record class, so the
// type arguments have to be propagated through the `implements` clause.

package recordpatinfer;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class SupertypeInference {

  record Producer<T>(T value) implements Supplier<T> {
    @Override
    public T get() {
      return value;
    }
  }

  /** R<alpha> has supertype Supplier<alpha>; <Supplier<@Untainted String> = Supplier<alpha>>. */
  static void throughInterface(Supplier<@Untainted String> s) {
    if (s instanceof Producer(var v)) {
      @Untainted String vv = v;
    }
  }

  static void throughInterfaceTainted(Supplier<@Tainted String> s) {
    if (s instanceof Producer(var v)) {
      // :: error: (assignment)
      @Untainted String vv = v;
    }
  }

  /**
   * The supertype's type argument is a parameterized type mentioning the record's type parameter,
   * so the constraint <Supplier<List<@Untainted String>> = Supplier<List<alpha>>> has to be reduced
   * structurally to get alpha.
   */
  record ListProducer<T>(T value) implements Supplier<List<T>> {
    @Override
    public List<T> get() {
      throw new UnsupportedOperationException();
    }
  }

  static void nestedSupertypeArgument(Supplier<List<@Untainted String>> s) {
    if (s instanceof ListProducer(var v)) {
      @Untainted String vv = v;
    }
  }

  static void nestedSupertypeArgumentTainted(Supplier<List<@Tainted String>> s) {
    if (s instanceof ListProducer(var v)) {
      // :: error: (assignment)
      @Untainted String vv = v;
    }
  }

  /**
   * Two type parameters, both constrained by the supertype. Note that the record's type parameters
   * appear in the supertype in a different order than they are declared.
   */
  record Swapped<A, B>(A a, B b) implements BiFunction<B, A, String> {
    @Override
    public String apply(B b2, A a2) {
      return "";
    }
  }

  static void reorderedTypeArguments(BiFunction<@Untainted String, @Tainted Number, String> f) {
    if (f instanceof Swapped(var a, var b)) {
      // B = @Untainted String, A = @Tainted Number
      @Untainted String bb = b;
      // :: error: (assignment)
      @Untainted Number aa = a;
    }
  }

  /**
   * The record has a type parameter that the supertype does not mention, so alpha2 gets no
   * constraint beyond its declared bound. Per step 5 it resolves to a fresh type variable, and per
   * step 6 the upward projection turns it into a wildcard: {@code Pair<@Untainted String, ?>}.
   */
  record Pair<A, B>(A a, B b) implements Supplier<A> {
    @Override
    public A get() {
      return a;
    }
  }

  static void unconstrainedTypeParameter(Supplier<@Untainted String> s) {
    if (s instanceof Pair(var a, var b)) {
      @Untainted String aa = a;
      // `b`'s type comes from a wildcard, so it is only known to be a (top-qualified) Object.
      @Tainted Object bb = b;
      // :: error: (assignment)
      @Untainted Object bad = b;
    }
  }

  /**
   * The unconstrained type parameter has a declared bound carrying a qualifier, so per step 2 the
   * initial bound set B0 already constrains it and the fresh type variable's upper bound is
   * {@code @Untainted CharSequence}.
   */
  record BoundedPair<A, B extends @Untainted CharSequence>(A a, B b) implements Supplier<A> {
    @Override
    public A get() {
      return a;
    }
  }

  static void boundedUnconstrainedTypeParameter(Supplier<@Tainted String> s) {
    if (s instanceof BoundedPair(var a, var b)) {
      @Untainted CharSequence bb = b;
      // :: error: (assignment)
      @Untainted String aa = a;
    }
  }

  /** The declared bound refers to the type parameter itself (an f-bound). */
  record SelfBounded<A extends Comparable<A>>(A a) implements Supplier<A> {
    @Override
    public A get() {
      return a;
    }
  }

  static void fBoundedTypeParameter(Supplier<@Untainted String> s) {
    if (s instanceof SelfBounded(var a)) {
      @Untainted String aa = a;
    }
  }

  static void fBoundedTypeParameterTainted(Supplier<@Tainted String> s) {
    if (s instanceof SelfBounded(var a)) {
      // :: error: (assignment)
      @Untainted String aa = a;
    }
  }
}
