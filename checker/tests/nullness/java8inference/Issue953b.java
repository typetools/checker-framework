// Test case that was submitted in Issue 953, but was combined with Issue 979
// https://github.com/typetools/checker-framework/issues/979

// @below-java8-jdk-skip-test
// @skip-test until the bug is fixed

import java.util.*;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;

public class Issue953b {
    private static List<List<?>> strs = new ArrayList();

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
