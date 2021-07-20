import org.checkerframework.checker.nullness.qual.EnsuresKeyForIf;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.dataflow.qual.Pure;

import java.util.HashMap;
import java.util.Map;

public class Issue2619 {
    public Map<String, String> map = new HashMap<>();

    void m00(Aux aux1) {
        if (aux1.hasValue(Aux.MINIMUM_VALUE)) {
            @KeyFor({"aux1.map"}) String s1 = Aux.MINIMUM_VALUE;
        }
    }

    void m01(Aux aux1, Aux aux2) {
        if (aux1.hasValue(Aux.MINIMUM_VALUE) && aux2.hasValue(Aux.MINIMUM_VALUE)) {
            @KeyFor({"aux1.map", "aux2.map"}) String s1 = Aux.MINIMUM_VALUE;
        }
    }

    void m02(Aux aux1, Aux aux2) {
        if (aux1.hasValue(Aux.MINIMUM_VALUE) && map.containsKey(Aux.MINIMUM_VALUE)) {
            @KeyFor({"aux1.map", "map"}) String s1 = Aux.MINIMUM_VALUE;
        }
    }

    void m03(Aux aux1, Aux aux2) {
        if (map.containsKey(Aux.MINIMUM_VALUE) && aux1.hasValue(Aux.MINIMUM_VALUE)) {
            @KeyFor({"aux1.map", "map"}) String s1 = Aux.MINIMUM_VALUE;
        }
    }

    static class Aux {

        public Map<String, String> map = new HashMap<>();

        public static final String MINIMUM_VALUE = "minvalue";

        @Pure
        @EnsuresKeyForIf(result = true, expression = "#1", map = "map")
        public boolean hasValue(String key) {
            return map.containsKey(key);
        }

        @Pure
        public int getInt(@KeyFor("this.map") String key) {
            return 22;
        }
    }
}
