import org.checkerframework.checker.nullness.qual.EnsuresKeyFor;
import org.checkerframework.checker.nullness.qual.EnsuresKeyForIf;

import java.util.HashMap;
import java.util.Map;

public class RepeatEnsuresKeyFor {

    Map<String, Integer> map = new HashMap<>();

    public void func1(String a, String b, String c) {
        map.put(a, 1);
        map.put(b, 2);
        map.put(c, 3);
    }

    public boolean func2(String a, String b, String c) {
        map.put(a, 1);
        map.put(b, 2);
        map.put(c, 3);
        return true;
    }

    @EnsuresKeyFor(
            value = {"#1", "#2"},
            map = "map")
    @EnsuresKeyFor(value = "#3", map = "map")
    public void client1(String a, String b, String c) {
        withpostconditionsfunc1(a, b, c);
    }

    @EnsuresKeyFor(
            value = {"#1", "#2"},
            map = "map")
    @EnsuresKeyFor(value = "#3", map = "map")
    public void client2(String a, String b, String c) {
        withpostconditionfunc1(a, b, c);
    }

    @EnsuresKeyForIf(
            expression = {"#1", "#2"},
            map = "map",
            result = true)
    @EnsuresKeyForIf(expression = "#3", map = "map", result = true)
    public boolean client3(String a, String b, String c) {
        return withcondpostconditionsfunc2(a, b, c);
    }

    @EnsuresKeyForIf.List({
        @EnsuresKeyForIf(expression = "#1", map = "map", result = true),
        @EnsuresKeyForIf(expression = "#2", map = "map", result = true)
    })
    @EnsuresKeyForIf(expression = "#3", map = "map", result = true)
    public boolean client4(String a, String b, String c) {
        return withcondpostconditionfunc2(a, b, c);
    }

    @EnsuresKeyFor(
            value = {"#1", "#2"},
            map = "map")
    @EnsuresKeyFor(value = "#3", map = "map")
    public void withpostconditionsfunc1(String a, String b, String c) {
        map.put(a, 1);
        map.put(b, 2);
        map.put(c, 3);
    }

    @EnsuresKeyForIf(
            expression = {"#1", "#2"},
            map = "map",
            result = true)
    @EnsuresKeyForIf(expression = "#3", map = "map", result = true)
    public boolean withcondpostconditionsfunc2(String a, String b, String c) {
        map.put(a, 1);
        map.put(b, 2);
        map.put(c, 3);
        return true;
    }

    @EnsuresKeyFor.List({
        @EnsuresKeyFor(value = "#1", map = "map"),
        @EnsuresKeyFor(value = "#2", map = "map"),
    })
    @EnsuresKeyFor(value = "#3", map = "map")
    public void withpostconditionfunc1(String a, String b, String c) {
        map.put(a, 1);
        map.put(b, 2);
        map.put(c, 3);
    }

    @EnsuresKeyForIf.List({
        @EnsuresKeyForIf(expression = "#1", map = "map", result = true),
        @EnsuresKeyForIf(expression = "#2", map = "map", result = true)
    })
    @EnsuresKeyForIf(expression = "#3", map = "map", result = true)
    public boolean withcondpostconditionfunc2(String a, String b, String c) {
        map.put(a, 1);
        map.put(b, 2);
        map.put(c, 3);
        return true;
    }
}
