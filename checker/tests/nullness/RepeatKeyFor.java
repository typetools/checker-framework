import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.EnsuresKeyFor;
import org.checkerframework.checker.nullness.qual.EnsuresKeyForIf;

public class RepeatKeyFor {

    Map<String, Integer> map = new HashMap<>();
    Map<String, Integer> map2 = new HashMap<>();

    public void func1(String a, String b, String c) {
        map.put(a, 1);
        map.put(c, 2);
        map2.put(a, 3);
    }

    public boolean func2(String a, String b, String c) {
        map.put(a, 1);
        map.put(c, 2);
        map2.put(b, 3);
        return true;
    }

    @EnsuresKeyFor(
            value = {"#1", "#2"},
            map = "map")
    @EnsuresKeyFor(value = "#2", map = "map2")
    // :: error:  (contracts.postcondition.not.satisfied)
    public void samefunc1(String a, String b, String c) {
        map.put(a, 1);
        map.put(c, 2); // condition not satisfied here
        map2.put(a, 3);
    }

    @EnsuresKeyForIf(
            expression = {"#1", "#2"},
            map = "map",
            result = true)
    @EnsuresKeyForIf(expression = "#2", map = "map2", result = true)
    public boolean samefunc2(String a, String b, String c) {
        map.put(a, 1);
        map.put(c, 2); // condition not satisfied here
        map2.put(b, 3);
        // :: error:  (contracts.conditional.postcondition.not.satisfied)
        return true;
    }

    @EnsuresKeyFor.List({
        @EnsuresKeyFor(
                value = {"#1", "#2"},
                map = "map"),
        @EnsuresKeyFor(value = "#2", map = "map2")
    })
    @EnsuresKeyFor(value = "#3", map = "map2")
    public void func3(String a, String b, String c) {
        map.put(a, 1);
        map.put(b, 2);
        map2.put(b, 3);
        map2.put(c, 4);
    }

    @EnsuresKeyForIf.List({
        @EnsuresKeyForIf(
                expression = {"#1", "#2"},
                map = "map",
                result = true),
        @EnsuresKeyForIf(expression = "#2", map = "map2", result = true)
    })
    @EnsuresKeyForIf(expression = "#3", map = "map2", result = true)
    public boolean func4(String a, String b, String c) {
        map.put(a, 1);
        map.put(b, 2);
        map2.put(b, 3);
        map2.put(c, 4);
        return true;
    }
}
