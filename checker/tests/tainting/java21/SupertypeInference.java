// @below-java21-jdk-skip-test

// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Tests for step 4 of JLS 26 section 18.5.5: if T' is a parameterization of a generic class G and
// some supertype R' of R<alpha1,...,alphan> is also a parameterization of G, then the constraint
// formula <T' = R'> is reduced.  Here the matched type is a supertype of the record class, so the
// type arguments have to be propagated through the `implements` clause.

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

  /**
   * Producer<alpha> has supertype Supplier<alpha>, so inference reduces the constraint
   * <Supplier<@Untainted String> = Supplier<alpha>>.
   */
  static void throughInterface(Supplier<@Untainted String> s) {
    if (s instanceof Producer(var v)) {
      @Untainted String vv = v;
    }
  }

  static void throughInterfaceTainted(Supplier<@Tainted String> s) {
    if (s instanceof Producer(var v)) {
      // :: error: [assignment]
      @Untainted String vv = v;
    }
  }

  /**
   * A record whose supertype's type argument is a parameterized type that mentions the record's
   * type parameter.
   */
  record ListProducer<T>(T value) implements Supplier<List<T>> {
    @Override
    public List<T> get() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Inference has to reduce the constraint <Supplier<List<@Untainted String>> =
   * Supplier<List<alpha>>> structurally to get alpha.
   */
  static void nestedSupertypeArgument(Supplier<List<@Untainted String>> s) {
    if (s instanceof ListProducer(var v)) {
      @Untainted String vv = v;
    }
  }

  static void nestedSupertypeArgumentTainted(Supplier<List<@Tainted String>> s) {
    if (s instanceof ListProducer(var v)) {
      // :: error: [assignment]
      @Untainted String vv = v;
    }
  }

  /**
   * Two type parameters, both constrained by the supertype. The record's type parameters appear in
   * the supertype in a different order than they are declared.
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
      // :: error: [assignment]
      @Untainted Number aa = a;
    }
  }

  /** A record with a type parameter, B, that its supertype does not mention. */
  record Pair<A, B>(A a, B b) implements Supplier<A> {
    @Override
    public A get() {
      return a;
    }
  }

  /**
   * The inference variable for B, alpha2, gets no constraint beyond the declared bound of B. Per
   * step 5, alpha2 resolves to a fresh type variable, and per step 6 the upward projection replaces
   * that type variable with a wildcard: {@code Pair<@Untainted String, ?>}.
   */
  static void unconstrainedTypeParameter(Supplier<@Untainted String> s) {
    if (s instanceof Pair(var a, var b)) {
      @Untainted String aa = a;
      // `b`'s type comes from a wildcard, so it is only known to be a (top-qualified) Object.
      @Tainted Object bb = b;
      // :: error: [assignment]
      @Untainted Object bad = b;
    }
  }

  /**
   * A record with a type parameter, B, that its supertype does not mention and whose declared bound
   * has a qualifier.
   */
  record BoundedPair<A, B extends @Untainted CharSequence>(A a, B b) implements Supplier<A> {
    @Override
    public A get() {
      return a;
    }
  }

  /**
   * Per step 2, the initial bound set B0 already constrains alpha2, the inference variable for B,
   * so the upper bound of the fresh type variable that alpha2 resolves to is {@code @Untainted
   * CharSequence}.
   */
  static void boundedUnconstrainedTypeParameter(Supplier<@Tainted String> s) {
    if (s instanceof BoundedPair(var a, var b)) {
      @Untainted CharSequence bb = b;
      // :: error: [assignment]
      @Untainted String aa = a;
    }
  }

  /** The declared bound refers to the type parameter itself (an F-bound). */
  record SelfBounded<A extends Comparable<A>>(A a) implements Supplier<A> {
    @Override
    public A get() {
      return a;
    }
  }

  /**
   * String's supertype {@code Comparable<String>} is {@code Comparable<@Tainted String>}, so
   * {@code @Untainted String} does not satisfy the F-bound, just as {@code SelfBounded<@Untainted
   * String>} is not a valid type.
   */
  static void fBoundedTypeParameter(Supplier<@Untainted String> s) {
    // :: error: [type.arguments.not.inferred]
    if (s instanceof SelfBounded(var a)) {
      // :: error: [assignment]
      @Untainted String aa = a;
    }
  }

  static void fBoundedTypeParameterTainted(Supplier<@Tainted String> s) {
    if (s instanceof SelfBounded(var a)) {
      // :: error: [assignment]
      @Untainted String aa = a;
    }
  }

  interface Two<X, Y> {}

  /** A record whose supertype mentions the record's type parameter twice. */
  record Diag<T>(T a) implements Two<T, T> {}

  /**
   * Inference makes alpha and the inference variables for both wildcards equal to one another, so
   * resolution must give all three the same type.
   */
  static void equalInferenceVariables(Two<? extends @Untainted Object, ?> o) {
    if (o instanceof Diag(var a)) {
      @Untainted Object aa = a;
    }
  }

  static void equalInferenceVariablesTainted(Two<?, ?> o) {
    if (o instanceof Diag(var a)) {
      // :: error: [assignment]
      @Untainted Object aa = a;
    }
  }
}
