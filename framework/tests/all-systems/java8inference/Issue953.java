// Test case that was submitted in Issue 953, but was combined with Issue 979
// https://github.com/typetools/checker-framework/issues/979

// @skip-test until the bug is fixed

import java.util.*;
import java.util.stream.Collectors;

public class Issue953 {
    public static void test() {
        List<String> initial = new ArrayList<>();
        List<Integer> counts =
                initial.stream().skip(1).map(l -> count("", l)).collect(Collectors.toList());
    }

    private static int count(String s, String l) {
        return 0;
    }
}
