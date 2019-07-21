import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.EnsuresKeyFor;
import org.checkerframework.checker.nullness.qual.EnsuresKeyForIf;

public class RepeatKeyForWithError {

    Map<String, Integer> map = new HashMap<>();
    Map<String, Integer> map2 = new HashMap<>();

    public void func1(String a, String b, String c) {
        map.put(a, 1);
        map.put(b, 2);
        map2.put(b, 3);
    }

    public boolean func2(String a, String b, String c) {
        map.put(a, 1);
        map.put(b, 2);
        map2.put(b, 3);
        return true;
    }

    @EnsuresKeyFor(
            value = {"#1", "#2"},
            map = "map")
    @EnsuresKeyFor(value = "#3", map = "map2")
    public void client1(String a, String b, String c) {
        withpostconditionsfunc1(a, b, c);
    }

    @EnsuresKeyFor(
            value = {"#1", "#2"},
            map = "map")
    @EnsuresKeyFor(value = "#3", map = "map2")
    public void client2(String a, String b, String c) {
        withpostconditionfunc1(a, b, c);
    }

    @EnsuresKeyForIf(
            expression = {"#1", "#2"},
            map = "map",
            result = true)
    @EnsuresKeyForIf(expression = "#3", map = "map2", result = true)
    public boolean client3(String a, String b, String c) {
        return withcondpostconditionsfunc2(a, b, c);
    }

    @EnsuresKeyForIf.List({
        @EnsuresKeyForIf(expression = "#1", map = "map", result = true),
        @EnsuresKeyForIf(expression = "#2", map = "map", result = true)
    })
    @EnsuresKeyForIf(expression = "#3", map = "map2", result = true)
    public boolean client4(String a, String b, String c) {
        return withcondpostconditionfunc2(a, b, c);
    }

    @EnsuresKeyFor(
            value = {"#1", "#2"},
            map = "map")
    @EnsuresKeyFor(value = "#3", map = "map2")
    // :: error:  (contracts.postcondition.not.satisfied)
    public void withpostconditionsfunc1(String a, String b, String c) {
        map.put(a, 1);
        map.put(b, 2);
        map2.put(b, 3); // condition not satisfied here
    }

    @EnsuresKeyForIf(
            expression = {"#1", "#2"},
            map = "map",
            result = true)
    @EnsuresKeyForIf(expression = "#3", map = "map2", result = true)
    public boolean withcondpostconditionsfunc2(String a, String b, String c) {
        map.put(a, 1);
        map.put(b, 2);
        map2.put(b, 3);
        // :: error:  (contracts.conditional.postcondition.not.satisfied)
        return true;
    }

    @EnsuresKeyFor.List({
        @EnsuresKeyFor(value = "#1", map = "map"),
        @EnsuresKeyFor(value = "#2", map = "map"),
    })
    @EnsuresKeyFor(value = "#3", map = "map2")
    // :: error:  (contracts.postcondition.not.satisfied)
    public void withpostconditionfunc1(String a, String b, String c) {
        map.put(a, 1);
        map.put(b, 2);
        map2.put(b, 3); // condition not satisfied here
    }

    @EnsuresKeyForIf.List({
        @EnsuresKeyForIf(expression = "#1", map = "map", result = true),
        @EnsuresKeyForIf(expression = "#2", map = "map", result = true)
    })
    @EnsuresKeyForIf(expression = "#3", map = "map2", result = true)
    public boolean withcondpostconditionfunc2(String a, String b, String c) {
        map.put(a, 1);
        map.put(b, 2);
        map2.put(b, 3);
        // :: error:  (contracts.conditional.postcondition.not.satisfied)
        return true;
    }
}
