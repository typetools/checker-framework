import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.checkerframework.checker.modifiability.qual.Modifiable;

public class ConcurrentHashMapModifiableTest {

  void testBasicOperations() {
    // 1. Class-level: it's a modifiable map
    @Modifiable ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
    map.put("one", "1");
    map.remove("one");
  }

  void testIteratorEntries() {
    @Modifiable ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
    map.put("one", "1");

    // 2. entrySet() / iterators / spliterators: entries are mutable
    for (Map.Entry<String, String> entry : map.entrySet()) {
      // Iterator returns modifiable entries for ConcurrentHashMap
      entry.setValue("2"); // OK
    }

    // Explicit iterator usage
    Map.Entry<String, String> explicitEntry = map.entrySet().iterator().next();
    explicitEntry.setValue("3"); // OK
  }

  void testBulkOperations() {
    @Modifiable ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
    map.put("one", "1");

    // 3. Bulk operations on Map.Entry objects do not support method setValue

    // forEachEntry
    map.forEachEntry(
        1,
        entry -> {
          // :: error: [method.invocation]
          entry.setValue("3");
        });

    // searchEntries
    map.searchEntries(
        1,
        entry -> {
          // :: error: [method.invocation]
          entry.setValue("3");
          return "result";
        });

    // reduceEntries
    map.reduceEntries(
        1,
        entry -> {
          // :: error: [method.invocation]
          entry.setValue("3");
          return "result";
        },
        (a, b) -> a);

    // reduceEntriesToDouble
    map.reduceEntriesToDouble(
        1,
        entry -> {
          // :: error: [method.invocation]
          entry.setValue("3");
          return 0.0;
        },
        0.0,
        Double::sum);

    // reduceEntriesToInt
    map.reduceEntriesToInt(
        1,
        entry -> {
          // :: error: [method.invocation]
          entry.setValue("3");
          return 0;
        },
        0,
        Integer::sum);

    // reduceEntriesToLong
    map.reduceEntriesToLong(
        1,
        entry -> {
          // :: error: [method.invocation]
          entry.setValue("3");
          return 0L;
        },
        0L,
        Long::sum);

    // reduceEntries (Entry reducer) - safe read-only usage
    map.reduceEntries(1, (e1, e2) -> e1.getValue().length() >= e2.getValue().length() ? e1 : e2);

    // forEachEntry - safe read-only usage
    map.forEachEntry(
        1,
        entry -> {
          if (entry.getValue().length() > 0) {
            // read-only access
          }
        });

    // searchEntries - safe read-only usage
    map.searchEntries(1, entry -> entry.getValue().length() > 5 ? entry.getKey() : null);
  }

  // pickFirst function can take in any Map.Entry mutability.
  public static <K, V> Map.Entry<K, V> pickFirst(Map.Entry<K, V> a, Map.Entry<K, V> b) {
    return a; // only reads, no setValue
  }

  void invarianceDemo() {
    @Modifiable ConcurrentHashMap<String, String> m = new ConcurrentHashMap<>();
    m.put("k1", "v1");
    m.put("k2", "v2");

    // To verify it compiles/runs in this environment:
    Map.Entry<String, String> e = m.reduceEntries(1L, ConcurrentHashMapModifiableTest::pickFirst);
  }
}
