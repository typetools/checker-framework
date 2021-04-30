import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.EnsuresKeyFor;
import org.checkerframework.checker.nullness.qual.EnsuresKeyForIf;
import org.checkerframework.checker.nullness.qual.KeyFor;

public class KeyForPostcondition {

  public static Map<String, Integer> m = new HashMap<>();

  // public static @KeyFor("m") String key = "hello";

  public static boolean b;

  @EnsuresKeyFor(value = "#1", map = "m")
  public void putKey(String x) {
    m.put(x, 22);
  }

  public void usePutKey(String x) {
    // :: error: (assignment)
    @KeyFor("m") String a = x;
    putKey(x);
    @KeyFor("m") String b = x;
  }

  @EnsuresKeyForIf(expression = "#1", result = true, map = "m")
  public boolean tryPutKey(String x) {
    if (b) {
      putKey(x);
      return true;
    } else {
      return false;
    }
  }

  public void useTryPutKey(String x) {
    // :: error: (assignment)
    @KeyFor("m") String a = x;
    if (tryPutKey(x)) {
      @KeyFor("m") String b = x;
    }

    // :: error: (assignment)
    @KeyFor("m") String c = x;
  }
}
