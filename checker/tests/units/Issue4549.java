import java.util.HashMap;
import java.util.Map;

class Issue4549 {
  private Map<String, Long> map = new HashMap<>();

  void testMethod(String s, int i) {
    Long l = map.get(s);
    l += i;
  }
}
