package beamcrash;

/**
 * A diamond {@code new Foo<>(...)} passed as an argument to a generic method invoked on a
 * <b>raw</b> receiver crashed with {@code StructuralEqualityComparer: unexpected combination:
 * type1: [DECLARED ...] @Tainted String type2: [TYPEVAR ...] T extends @Tainted Object}, thrown
 * from {@code AtmComboVisitor.defaultAction} by way of {@code
 * DefaultTypeHierarchy.isContainedBy}/{@code BaseTypeVisitor.checkArguments}.
 *
 * <p>Reduced from Apache Beam, where it appears at two call sites, both invoking the generic {@code
 * KeyedStateBackend#getOrCreateKeyedState} on a field declared with the raw type {@code
 * KeyedStateBackend}:
 *
 * <ul>
 *   <li>{@code
 *       runners/flink/src/main/java/org/apache/beam/runners/flink/translation/wrappers/streaming/ExecutableStageDoFnOperator.java}
 *       line 1315, {@code #initializeUserState} -- {@code new ListStateDescriptor<>(...)}.
 *   <li>{@code
 *       runners/flink/src/main/java/org/apache/beam/runners/flink/translation/wrappers/streaming/state/FlinkStateInternals.java}
 *       line 1751, {@code EarlyBinder#bindSet} -- {@code new MapStateDescriptor<>(...)}.
 * </ul>
 *
 * <p>The raw receiver erases the invoked method's signature, so its formal parameter type is the
 * erasure while the diamond's inferred type argument is not, and comparing the two reaches {@code
 * StructuralEqualityComparer} with a declared type on one side and an uninstantiated type variable
 * on the other.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Issue8054 {

  interface Ser<T> {}

  static class Desc<T> {
    Desc(Ser<T> s) {}
  }

  interface Backend<K> {
    <T> void get(Desc<T> d);
  }

  // Minimal form.
  void minimal(Backend raw, Ser<String> ser) {
    raw.get(new Desc<>(ser));
  }

  // The raw receiver is usually a field, as it is in both Beam call sites.
  private Backend rawField;

  Issue8054(Backend rawField) {
    this.rawField = rawField;
  }

  void rawReceiverIsAField(Ser<String> ser) {
    rawField.get(new Desc<>(ser));
  }

  // The Beam shapes, with the surrounding structure those call sites actually have: a bounded
  // method type variable, a second argument, and a descriptor that reaches the parameter type
  // through a supertype.

  interface State {}

  interface ListState<T> extends State {}

  interface MapState<K1, V1> extends State {}

  static class StateDescriptor<S, T> {}

  static class ListStateDescriptor<T> extends StateDescriptor<ListState<T>, java.util.List<T>> {
    ListStateDescriptor(String name, Ser<T> ser) {}
  }

  static class MapStateDescriptor<UK, UV>
      extends StateDescriptor<MapState<UK, UV>, java.util.Map<UK, UV>> {
    MapStateDescriptor(String name, Ser<UK> k, Ser<UV> v) {}
  }

  static class BooleanSerializer implements Ser<Boolean> {
    static final BooleanSerializer INSTANCE = new BooleanSerializer();
  }

  interface KeyedStateBackend<KeyT> {
    <N, S extends State, T> S getOrCreateKeyedState(
        Ser<N> namespaceSerializer, StateDescriptor<S, T> descriptor) throws Exception;
  }

  // ExecutableStageDoFnOperator#initializeUserState
  void beamListStateDescriptor(KeyedStateBackend raw, Ser<Object> ns, Ser<String> elem)
      throws Exception {
    raw.getOrCreateKeyedState(ns, new ListStateDescriptor<>("id", elem));
  }

  // FlinkStateInternals.EarlyBinder#bindSet
  void beamMapStateDescriptor(KeyedStateBackend raw, Ser<Object> ns, Ser<String> elem)
      throws Exception {
    raw.getOrCreateKeyedState(ns, new MapStateDescriptor<>("id", elem, BooleanSerializer.INSTANCE));
  }

  // The same shape with a generic *constructor* on a raw class rather than a generic method on a
  // raw receiver.  This crashed identically before the fix, and it exercises the NEW_CLASS case of
  // DefaultTypeArgumentInference.outerInference rather than the METHOD_INVOCATION case.

  static class Holder<K> {
    <T> Holder(Desc<T> d) {}
  }

  void rawGenericConstructor(Ser<String> ser) {
    Holder unused = new Holder(new Desc<>(ser));
  }

  // A generic constructor of a *raw superclass*, invoked through an explicit super(...) call.
  // super(...) has no receiver tree, so the enclosing class is passed to
  // TypesUtils.isRawCall, which walks up to the superclass.  (RawSuper.java covers a raw
  // superclass whose constructor is not generic, which does not reach this code path.)

  static class Sup<K> {
    <U> Sup(Desc<U> d) {}
  }

  static class SubRaw extends Sup {
    SubRaw(Ser<String> ser) {
      super(new Desc<>(ser));
    }
  }

  // Contrast: the superclass is parameterized rather than raw, so the outer inference still
  // applies to the super(...) call.
  static class SubParameterized extends Sup<Object> {
    SubParameterized(Ser<String> ser) {
      super(new Desc<>(ser));
    }
  }

  // Variants that did not crash before the fix, to pin down which ingredients are required and to
  // check that declining the outer inference does not change their results.

  // An explicit type argument instead of a diamond.
  void explicitTypeArg(Backend raw, Ser<String> ser) {
    raw.get(new Desc<String>(ser));
  }

  // A diamond, but the receiver is not raw, so the outer inference still applies.
  void nonRawReceiver(Backend<Object> notRaw, Ser<String> ser) {
    notRaw.get(new Desc<>(ser));
  }

  // A raw receiver, but the method is not generic.
  interface BackendNonGeneric<K> {
    void get(Desc<String> d);
  }

  void nonGenericMethod(BackendNonGeneric raw, Ser<String> ser) {
    raw.get(new Desc<>(ser));
  }

  // A raw receiver and a generic method, but the diamond's parameter does not mention the method's
  // type variable.
  interface BackendUnrelatedTypeVar<K> {
    <T> void get(Desc<String> d, T unrelated);
  }

  void paramDoesNotMentionTypeVar(BackendUnrelatedTypeVar raw, Ser<String> ser) {
    raw.get(new Desc<>(ser), "x");
  }

  // A static method of a raw type keeps its generic type (JLS 4.8), so the outer inference must
  // still apply here.
  static class StaticHolder<K> {
    static <T> void staticGet(Desc<T> d) {}
  }

  void staticMethodOfRawType(Ser<String> ser) {
    StaticHolder.staticGet(new Desc<>(ser));
  }
}
