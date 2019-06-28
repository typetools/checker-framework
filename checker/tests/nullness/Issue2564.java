import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.KeyFor;

// https://github.com/typetools/checker-framework/issues/2587
// Issue 2587 tracks the missing errors.
@SuppressWarnings("") // Just check for crashes.
public abstract class Issue2564 {
    public enum EnumType {
        // TODO:: error: (expression.unparsable.type.invalid) :: error:
        // (assignment.type.incompatible)
        @KeyFor("myMap") MY_KEY,
        // TODO:: error: (assignment.type.incompatible)
        @KeyFor("enumMap") ENUM_KEY;
        private static final Map<String, Integer> enumMap = new HashMap<>();

        void method() {
            @KeyFor("enumMap") EnumType t = ENUM_KEY;
            int x = enumMap.get(ENUM_KEY);
        }
    }

    public static class Inner {
        // TODO:: error: (expression.unparsable.type.invalid)
        @KeyFor("myMap") String MY_KEY = "";
    }

    private final Map<String, Integer> myMap = new HashMap<>();
}
