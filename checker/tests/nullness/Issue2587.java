import org.checkerframework.checker.nullness.qual.KeyFor;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("assignment.type.incompatible") // These warnings are not relevant
public abstract class Issue2587 {
    public enum EnumType {
        // :: error: (expression.unparsable.type.invalid)
        @KeyFor("myMap") MY_KEY,

        @KeyFor("enumMap") ENUM_KEY;
        private static final Map<String, Integer> enumMap = new HashMap<>();

        void method() {
            @KeyFor("enumMap") EnumType t = ENUM_KEY;
            int x = enumMap.get(ENUM_KEY);
        }
    }

    public static class Inner {
        // :: error: (expression.unparsable.type.invalid)
        @KeyFor("myMap") String MY_KEY = "";

        public static class Inner2 {
            // :: error: (expression.unparsable.type.invalid)
            @KeyFor("myMap") String MY_KEY2 = "";

            @KeyFor("innerMap") String MY_KEY3 = "";
        }

        private static final Map<String, Integer> innerMap = new HashMap<>();
    }

    private final Map<String, Integer> myMap = new HashMap<>();
}
