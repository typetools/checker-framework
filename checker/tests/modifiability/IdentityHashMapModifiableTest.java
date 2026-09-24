import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import org.checkerframework.checker.modifiability.qual.Modifiable;

public class IdentityHashMapModifiableTest {

  void testIdentityHashMap() {
    @Modifiable IdentityHashMap<String, String> identityMap = new IdentityHashMap<>();
    identityMap.put("key", "value1");
    identityMap.remove("key");

    // IdentityHashMap itself is mutable
    boolean removed = identityMap.remove("key", "value2");
  }

  // TODO: The checker does not yet know that a map's keySet(), values(), and entrySet() views
  // support removal but not addition.

  void testEntries() {
    @Modifiable IdentityHashMap<String, String> identityMap = new IdentityHashMap<>();
    identityMap.put("k", "v");

    // Iterator returns mutable entries
    Iterator<Map.@Modifiable Entry<String, String>> it = identityMap.entrySet().iterator();
    if (it.hasNext()) {
      Map.Entry<String, String> entry = it.next();
      // the following method is allowed and works at runtime
      entry.setValue("modified"); // OK
    }

    // Stream/Spliterator returns immutable entries
    if (!identityMap.isEmpty()) {
      Map.Entry<String, String> entry = identityMap.entrySet().stream().findFirst().get();
      // TODO: This throws UnsupportedOperationException at run time, but the checker gives
      // entrySet().stream() the same modifiability as entrySet().iterator(), so the checker is
      // unsound here.
      entry.setValue("modified");
    }
  }
}
