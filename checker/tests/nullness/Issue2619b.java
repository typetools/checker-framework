import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.EnsuresKeyForIf;
import org.checkerframework.checker.nullness.qual.KeyFor;

public class Issue2619b {
  public Map<String, String> map = new HashMap<>();

  void m01(Aux aux1, Aux aux2) {
    if (aux1.hasValue(Aux.MINIMUM_VALUE) && aux2.hasValue(Aux.MINIMUM_VALUE)) {
      // hasValue is not side-effect-free, so the @KeyFor("aux1.map") is cleared rather than glb'ed.
      // :: error: (assignment)
      @KeyFor({"aux1.map", "aux2.map"}) String s1 = Aux.MINIMUM_VALUE;
    }
  }

  void m02(Aux aux1, Aux aux2) {
    if (aux1.hasValue(Aux.MINIMUM_VALUE) && aux2.hasValue(Aux.MINIMUM_VALUE)) {
      @KeyFor("aux2.map") String s1 = Aux.MINIMUM_VALUE;
    }
  }

  void m03(Aux aux1, Aux aux2) {
    if (aux1.hasValue(Aux.MINIMUM_VALUE) && map.containsKey(Aux.MINIMUM_VALUE)) {
      // ok because map.containsKey is side-effect-free.
      @KeyFor({"aux1.map", "map"}) String s1 = Aux.MINIMUM_VALUE;
      @KeyFor("map") String s2 = Aux.MINIMUM_VALUE;
    }
  }

  void m04(Aux aux1, Aux aux2) {
    if (map.containsKey(Aux.MINIMUM_VALUE) && aux1.hasValue(Aux.MINIMUM_VALUE)) {
      // :: error: (assignment)
      @KeyFor({"aux1.map", "map"}) String s1 = Aux.MINIMUM_VALUE;
      @KeyFor("aux1.map") String s2 = Aux.MINIMUM_VALUE;
    }
  }

  static class Aux {

    public Map<String, String> map = new HashMap<>();

    public static String MINIMUM_VALUE = "minvalue";

    @EnsuresKeyForIf(result = true, expression = "#1", map = "map")
    public boolean hasValue(String key) {
      return map.containsKey(key);
    }

    public int getInt(@KeyFor("this.map") String key) {
      return 22;
    }
  }
}
