import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue1922 {
  // A method to find a K in the collection and return it, or return null.
  public static <K> @Nullable K findKey(Collection<@NonNull K> keys, Object target) {
    for (K key : keys) {
      if (target.equals(key)) {
        return key;
      }
    }
    return null;
  }

  // Find a key in a map and return String version of its value.
  public static String findKeyAndFetchString(Map<String, Object> someMap) {
    // :: error: (type.argument.type.incompatible)
    @Nullable @KeyFor("someMap") String myKey = Issue1922.<@KeyFor("someMap") String>findKey(someMap.keySet(), "Foo");

    // :: error: (argument.type.incompatible)
    Object value = someMap.get(myKey);
    return value.toString();
  }

  public static void main(String[] args) {
    findKeyAndFetchString(new HashMap<>());
  }
}
