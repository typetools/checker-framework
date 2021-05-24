import java.io.PrintStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class NonNullMapValue {

  // Discussion:
  //
  // It can be useful to indicate that all the values in a map are non-null.
  // ("@NonNull" is redundant in this declaration, but I've written it
  // explicitly because it is the annotation we are talking about.)
  //   HashMap<String,@NonNull String> myMap;
  //
  // However, the get method's declaration is misleading (in the context of the nullness type
  // system), since it can always return null no matter whether the map values are non-null:
  //   V get(Object key) { ... return null; }
  //
  // Here are potential solutions:
  //  * Forbid declaring values as non-null.  This is the wrong approach.  (It would also be hard to
  //    express syntactically.)
  //  * The checker could recognize a special new annotation on the return value of get, indicating
  //    that its return type isn't merely inferred from the generic type, but is always nullable.
  //    (This special new annotations could even be "@Nullable".  A different annotation may be
  //    better, becase in general we would like to issue an error message when someone applies an
  //    annotation to a generic type parameter.)
  // Additionally, to reduce the number of false positive warnings caused by the fact that get's
  // return value is nullable:
  //  * Build a more specialized sophisticated flow analysis that checks that the passed key to
  //    Map.containsKey() is either checked against Map.containsKey() or Map.keySet().

  Map<String, @NonNull String> myMap;

  NonNullMapValue(Map<String, @NonNull String> myMap) {
    this.myMap = myMap;
  }

  void testMyMap(String key) {
    @NonNull String value;
    // :: error: (assignment)
    value = myMap.get(key); // should issue warning
    if (myMap.containsKey(key)) {
      value = myMap.get(key);
    }
    for (String keyInMap : myMap.keySet()) {
      // :: error: (assignment)
      value = myMap.get(key); // should issue warning
    }
    for (String keyInMap : myMap.keySet()) {
      value = myMap.get(keyInMap);
    }
    for (Map.Entry<@KeyFor("myMap") String, @NonNull String> entry : myMap.entrySet()) {
      String keyInMap = entry.getKey();
      value = entry.getValue();
    }
    for (Iterator<@KeyFor("myMap") String> iter = myMap.keySet().iterator(); iter.hasNext(); ) {
      String keyInMap = iter.next();
      // value = myMap.get(keyInMap);
    }
    value = myMap.containsKey(key) ? myMap.get(key) : "hello";
  }

  public static <T extends @NonNull Object> void print(
      Map<T, List<T>> graph, PrintStream ps, int indent) {
    for (T node : graph.keySet()) {
      for (T child : graph.get(node)) {
        ps.printf("  %s%n", child);
      }
      @NonNull List<T> children = graph.get(node);
      for (T child : children) {
        ps.printf("  %s%n", child);
      }
    }
  }

  public static <T extends @NonNull Object> void testAssertFlow(Map<T, List<T>> preds, T node) {
    assert preds.containsKey(node);
    for (T pred : preds.get(node)) {}
  }

  public static <T extends @NonNull Object> void testContainsKey1(Map<T, List<T>> dom, T pred) {
    assert dom.containsKey(pred);
    // Both of the next two lines should type-check.  The second one won't
    // unless the checker knows that pred is a key in the map.
    List<T> dom_of_pred1 = dom.get(pred);
    @NonNull List<T> dom_of_pred2 = dom.get(pred);
  }

  public static <T extends @NonNull Object> void testContainsKey2(Map<T, List<T>> dom, T pred) {
    if (!dom.containsKey(pred)) {
      throw new Error();
    }
    // Both of the next two lines should type-check.  The second one won't
    // unless the checker knows that pred is a key in the map.
    List<T> dom_of_pred1 = dom.get(pred);
    @NonNull List<T> dom_of_pred2 = dom.get(pred);
  }

  public static void process_unmatched_procedure_entries() {
    HashMap<Integer, Date> call_hashmap = new HashMap<>();
    for (Integer i : call_hashmap.keySet()) {
      @NonNull Date d = call_hashmap.get(i);
    }
    Set<@KeyFor("call_hashmap") Integer> keys = call_hashmap.keySet();
    for (Integer i : keys) {
      @NonNull Date d = call_hashmap.get(i);
    }
    Set<@KeyFor("call_hashmap") Integer> keys_sorted =
        new TreeSet<@KeyFor("call_hashmap") Integer>(call_hashmap.keySet());
    for (Integer i : keys_sorted) {
      @NonNull Date d = call_hashmap.get(i);
    }
  }

  public static Object testPut(Map<Object, Object> map, Object key) {
    if (!map.containsKey(key)) {
      map.put(key, new Object());
    }
    return map.get(key);
  }

  public static Object testAssertGet(Map<Object, Object> map, Object key) {
    assert map.get(key) != null;
    return map.get(key);
  }

  public static Object testThrow(Map<Object, Object> map, Object key) {
    if (!map.containsKey(key)) {
      if (true) {
        return "m";
      } else {
        throw new RuntimeException();
      }
    }
    return map.get(key);
  }

  public void negateMap(Map<Object, Object> map, Object key) {
    if (!map.containsKey(key)) {
    } else {
      @NonNull Object v = map.get(key);
    }
  }

  public void withinElseInvalid(Map<Object, Object> map, Object key) {
    if (map.containsKey(key)) {
    } else {
      // :: error: (assignment)
      @NonNull Object v = map.get(key); // should issue warning
    }
  }

  // Map.get should be annotated as @org.checkerframework.dataflow.qual.Pure
  public static int mapGetSize(MyMap<Object, List<Object>> covered, Object file) {
    return (covered.get(file) == null) ? 0 : covered.get(file).size();
  }

  interface MyMap<K, V> extends Map<K, V> {
    // TODO: @AssertGenericNullnessIfTrue("get(#1)")
    @org.checkerframework.dataflow.qual.Pure
    public abstract boolean containsKey(@Nullable Object a1);

    // We get an override warning, because we do not use the annotated JDK in the
    // test suite. Ignore this.
    @SuppressWarnings("override.return")
    @org.checkerframework.dataflow.qual.Pure
    public @Nullable V get(@Nullable Object o);
  }

  private static final String KEY = "key";
  private static final String KEY2 = "key2";

  void testAnd(MyMap<String, String> map, MyMap<String, @Nullable String> map2) {
    if (map.containsKey(KEY)) {
      map.get(KEY).toString();
    }
    // :: warning: (nulltest.redundant)
    if (map.containsKey(KEY2) && map.get(KEY2).toString() != null) {}
    // :: error: (dereference.of.nullable) :: warning: (nulltest.redundant)
    if (map2.containsKey(KEY2) && map2.get(KEY2).toString() != null) {}
  }

  void testAndWithIllegalMapAnnotation(MyMap2<String, String> map) {
    if (map.containsKey(KEY)) {
      map.get(KEY).toString();
    }
    // :: warning: (nulltest.redundant)
    if (map.containsKey(KEY2) && map.get(KEY2).toString() != null) {
      // do nothing
    }
  }

  interface MyMap2<K, V> {
    @org.checkerframework.dataflow.qual.Pure
    // This annotation is not legal on containsKey in general.  If the Map is declared as (say)
    // Map<Object, @Nullable Object>, then get returns a nullable value.  We really want to say that
    // if containsKey returns non-null, then get returns V rather than @Nullable V, but I don't know
    // how to say that.
    @EnsuresNonNullIf(result = true, expression = "get(#1)")
    public abstract boolean containsKey(@Nullable Object a1);

    @org.checkerframework.dataflow.qual.Pure
    public abstract @Nullable V get(@Nullable Object a1);
  }

  interface MyMap3<K, V> {
    @org.checkerframework.dataflow.qual.Pure
    @EnsuresNonNullIf(result = true, expression = "get(#1)")
    // The following error is issued because, unlike in interface MyMap2,
    // this interface has no get() method.
    // :: error: (flowexpr.parse.error)
    boolean containsKey(@Nullable Object a1);
  }
}
