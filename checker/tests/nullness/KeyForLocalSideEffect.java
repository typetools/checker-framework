import java.util.HashMap;
import org.checkerframework.checker.nullness.qual.*;

public class KeyForLocalSideEffect {

  String k = "key";
  HashMap<String, Integer> m = new HashMap<>();

  void testContainsKeyForFieldKeyAndLocalMap() {
    HashMap<String, Integer> m_local = m;

    if (m_local.containsKey(k)) {
      @KeyFor("m_local") String s = k;
      havoc();
      // TODO: This should be an error, because s is no longer a key for m_local.
      @NonNull Integer val = m_local.get(s);
    }
  }

  void havoc() {
    m = new HashMap<>();
  }
}
