package beamcrash;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A generic method whose return type contains a wildcard, invoked as the body of a lambda passed to
 * {@code Stream.map}, crashed inference with {@code FalseBoundException: False bound for:
 * Constraint: @Tainted Object <: @Tainted Getter<? extends @Tainted Object, ? extends @Tainted
 * Object>}.
 *
 * <p>Reduced from Apache Beam, {@code
 * sdks/java/extensions/protobuf/src/main/java/org/apache/beam/sdk/extensions/protobuf/ProtoByteBuddyUtils.java}
 * line 486, {@code #getGetters}, which is {@code types.stream().map(t -> createGetter(t,
 * ...)).collect(Collectors.toList())} where {@code createGetter} is declared {@code static <ProtoT>
 * FieldValueGetter<@NonNull ProtoT, ?>}.
 *
 * <p>The lambda's result type is the result of a nested generic invocation, so it is inferred, and
 * it contains a wildcard. That type is lost -- the lower bound of {@code map}'s {@code R} comes out
 * as {@code Object} instead of {@code Getter<String, ?>}, producing the false bound {@code Object
 * <: Getter<?, ?>}.
 *
 * <p>Both halves are required: the method must be generic (see {@code notGenericOk}) and its return
 * type must contain a wildcard (see {@code noWildcardOk}). It does not matter whether the type
 * argument is actually inferred from an argument -- {@code noArgument} below has nothing to infer
 * {@code P} from and crashes just the same.
 */
public class Issue8053 {

  interface Getter<K, V> {}

  static native <P> Getter<P, ?> wild(P p);

  static native <P> Getter<P, Object> noWild(P p);

  // Minimal: a wildcard-returning generic method as an expression lambda body.
  static List<Getter<?, ?>> minimal(List<String> types) {
    return types.stream().map(t -> wild(t)).collect(Collectors.toList());
  }

  static final Map<String, List<Getter<?, ?>>> CACHE = new HashMap<>();

  static native <P> Getter<P, ?> createGetter(Class<P> clazz, String name);

  // The Beam shape: the whole thing is the mapping function of Map.computeIfAbsent, and the
  // Class argument is itself wildcard-typed.
  static <T> List<Getter<?, ?>> beamShape(Class<? super T> clazz, List<String> types, String key) {
    return CACHE.computeIfAbsent(
        key, c -> types.stream().map(t -> createGetter(clazz, t)).collect(Collectors.toList()));
  }

  // The wildcard on the Class parameter is not needed.
  static <T> List<Getter<?, ?>> noWildcardOnArgument(Class<T> clazz, List<String> types) {
    return types.stream().map(t -> createGetter(clazz, t)).collect(Collectors.toList());
  }

  // Nor does the type argument have to be inferable from an argument: P resolves to Object here,
  // and this crashes too.
  static native <P> Getter<P, ?> noArg();

  static List<Getter<?, ?>> noArgument(List<String> types) {
    return types.stream().map(t -> noArg()).collect(Collectors.toList());
  }

  // A block-bodied lambda: the body's target type is computed by the RETURN branch of
  // InferenceFactory.getTargetType rather than by its LAMBDA_EXPRESSION branch.  Both branches can
  // hand the body a target type that contains a capture variable.
  static List<Getter<?, ?>> blockBodiedLambda(List<String> types) {
    return types.stream()
        .map(
            t -> {
              return wild(t);
            })
        .collect(Collectors.toList());
  }

  // An upper-bounded wildcard in the return type, which takes a different branch of
  // VariableBounds#getWildcardConstraints.
  static native <P> Getter<P, ? extends Number> upperWild(P p);

  static List<Getter<?, ? extends Number>> upperBoundedWildcard(List<String> types) {
    return types.stream().map(t -> upperWild(t)).collect(Collectors.toList());
  }

  // A lower-bounded wildcard in the return type, which takes the third branch.  The target type
  // is not tightened to `List<Getter<?, ? super Number>>`, the way upperBoundedWildcard's is:
  // CaptureBound#incorporate gives the capture variable the wildcard's bound only for
  // `? extends T`, so the capture variable here has no lower bound and the tighter target fails
  // under the Value Checker.  See CapturedWildcardBound#twoLevelSuper for why the symmetric bound
  // is not added.
  static native <P> Getter<P, ? super Number> lowerWild(P p);

  static List<Getter<?, ?>> lowerBoundedWildcard(List<String> types) {
    return types.stream().map(t -> lowerWild(t)).collect(Collectors.toList());
  }

  // The wildcard is not the last type argument, so the capture variable that must be resolved
  // before the result variable is the first one.
  static native <P> Getter<?, P> wildFirst(P p);

  static List<Getter<?, ?>> wildcardFirst(List<String> types) {
    return types.stream().map(t -> wildFirst(t)).collect(Collectors.toList());
  }

  // Below: variants that do NOT crash, which locate the bug.

  // A wildcard-free return type is fine.
  static List<Getter<?, ?>> noWildcardOk(List<String> types) {
    return types.stream().map(t -> noWild(t)).collect(Collectors.toList());
  }

  // A non-generic method whose return type has a wildcard is fine.
  static native Getter<String, ?> plainGetter();

  static List<Getter<?, ?>> notGenericOk(List<String> types) {
    return types.stream().map(t -> plainGetter()).collect(Collectors.toList());
  }

  // Without the enclosing collect() -- i.e. with no outer inference to poison -- it is fine.
  static Stream<Getter<?, ?>> noCollectOk(List<String> types) {
    return types.stream().map(t -> wild(t));
  }
}
