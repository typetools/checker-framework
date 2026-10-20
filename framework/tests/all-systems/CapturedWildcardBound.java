import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A fresh type variable created by resolution for the capture of {@code ? extends T} used to get
 * upper bound {@code Object} rather than {@code T}, which made {@code twoLevel} below report
 *
 * <pre>{@code
 * error: [type.arguments.not.inferred] Could not infer type arguments for Stream.collect
 *   unsatisfiable constraint:
 *     Getter<String, capture#01 extends Object> <: Getter<? extends Object, ? extends Number>
 * }</pre>
 *
 * <p>JLS 18.3.2 implies only the bound {@code alphai <: Bi theta} for a capture bound, so the
 * wildcard's own bound is not among the upper bounds that JLS 18.4 uses to build the fresh type
 * variable. Capture conversion (JLS 5.1.10), which javac applies to the return type instead, gives
 * the capture variable the upper bound {@code glb(T, Bi theta)}. {@code CaptureBound#incorporate}
 * now adds that bound.
 *
 * <p>The two-level shape is required: the receiver {@code Stream.of(...)} has no target type, so
 * the inner invocation is resolved -- with capture -- before the outer invocation constrains its
 * result. Where the argument's type is used directly, as in {@code singleton} and {@code
 * throughMk}, real capture conversion applies and the bound was never lost.
 */
public class CapturedWildcardBound {

  interface Getter<K, V> {}

  static native <P> Getter<P, ? extends Number> upperWild(P p);

  static native <P> Getter<P, ? super Number> lowerWild(P p);

  static native <P> Getter<P, ?> unbounded(P p);

  // Minimal: the target type uses the wildcard's bound, so losing it is an error.
  static List<Getter<?, ? extends Number>> twoLevel() {
    return Stream.of(upperWild("s")).collect(Collectors.toList());
  }

  // A bound on the type parameter itself, which must be glb'd with the wildcard's bound.
  interface NumGetter<K, V extends Number> {}

  static native <P> NumGetter<P, ? extends Integer> boundedParam(P p);

  static List<NumGetter<?, ? extends Integer>> twoLevelBoundedParam() {
    return Stream.of(boundedParam("s")).collect(Collectors.toList());
  }

  // The wildcard's bound is itself a parameterized type mentioning an inference variable.
  static native <P> Getter<P, ? extends List<P>> upperWildOfVar(P p);

  static List<Getter<?, ? extends List<? extends String>>> twoLevelBoundMentionsVariable() {
    return Stream.of(upperWildOfVar("s")).collect(Collectors.toList());
  }

  // Below: shapes that already worked, kept as controls.

  // The symmetric bound is not added for a lower-bounded wildcard, so the capture variable here
  // still has no lower bound and the target type cannot be tightened to
  // `List<Getter<?, ? super Number>>`.  Adding it makes this shape pass, but it makes
  // Issue8053#lowerBoundedWildcard fail under the Value Checker: the super bound of the wildcard
  // is defaulted to the bottom qualifier where the capture variable is created, and to the top
  // qualifier where the re-inferred lambda body is checked against it.
  static List<Getter<?, ?>> twoLevelSuper() {
    return Stream.of(lowerWild("s")).collect(Collectors.toList());
  }

  static List<Getter<?, ?>> twoLevelUnbounded() {
    return Stream.of(unbounded("s")).collect(Collectors.toList());
  }

  static List<Getter<?, ?>> twoLevelLooseTarget() {
    return Stream.of(upperWild("s")).collect(Collectors.toList());
  }

  static Getter<?, ? extends Number> direct() {
    return upperWild("s");
  }

  static <X> X id(X x) {
    return x;
  }

  static Getter<?, ? extends Number> throughId() {
    return id(upperWild("s"));
  }

  static native <Z> List<Z> mk(Z z);

  static List<Getter<?, ? extends Number>> throughMk() {
    return mk(upperWild("s"));
  }

  static List<Getter<?, ? extends Number>> singleton() {
    return Collections.singletonList(upperWild("s"));
  }
}
