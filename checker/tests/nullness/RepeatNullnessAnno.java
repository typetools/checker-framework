import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.*;

public class RepeatNullnessAnno {

    protected @Nullable String value1;
    protected @Nullable String value2;
    protected @Nullable String value3;
    protected int v1;
    protected int v2;
    protected int v3;
    Map<String, Integer> map = new HashMap<>();
    Map<String, Integer> map2 = new HashMap<>();

    @EnsuresNonNull("value1")
    @EnsuresNonNull(value = {"value2", "value3"})
    public void func2() {
        value1 = "value1";
        value2 = "value2";
        value3 = "value3";
    }

    @EnsuresNonNullIf(
            expression = {"value1", "value2"},
            result = true)
    @EnsuresNonNullIf(expression = "value3", result = false)
    public boolean func1() {
        value1 = "value1";
        value2 = "value2";
        value3 = null;
        return true;
    }

    @EnsuresKeyFor(
            value = {"#1", "#2"},
            map = "map")
    @EnsuresKeyFor(value = "#2", map = "map2")
    public void func3(String a, String b) {
        map.put(a, 1);
        map.put(b, 2);
        map2.put(b, 3);
    }

    @EnsuresKeyForIf(
            expression = {"#1", "#2"},
            map = "map",
            result = true)
    @EnsuresKeyForIf(expression = "#2", map = "map2", result = true)
    public boolean func4(String a, String b) {
        map.put(a, 1);
        map.put(b, 2);
        map2.put(b, 3);
        return true;
    }
}
