// Test case that was submitted in Issue 953, but was combined with Issue 979
// https://github.com/typetools/checker-framework/issues/979

import java.util.*;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;

public class Issue953bInference {
    private static List<List<?>> strs = new ArrayList<>();

    public static <R, T> List<@NonNull R> mapList(
            List<@NonNull T> list, Function<@NonNull T, @NonNull R> func) {
        ArrayList<@NonNull R> r = new ArrayList<>(list.size());
        for (T t : list) r.add(func.apply(t));
        return r;
    }

    public static List<String> test() {
        return mapList(
                strs,
                s -> {
                    return new String();
                });
    }
}
