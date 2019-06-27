import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.KeyFor;

public abstract class Issue2564 {
    public enum EnumType {
        // :: error: (expression.unparsable.type.invalid)
        @KeyFor("myMap") MY_KEY,
        @KeyFor("enumMap") ENUM_KEY;
        private final Map<String, Integer> enumMap = new HashMap<>();

        void method() {
            @KeyFor("enumMap") EnumType t = ENUM_KEY;
            int x = enumMap.get(ENUM_KEY);
        }
    }

    public static class Inner {

        @KeyFor("myMap") String MY_KEY = "";
    }

    private final Map<String, Integer> myMap = new HashMap<>();
}
