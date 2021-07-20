// Partial test case for issue #1330: https://github.com/typetools/checker-framework/issues/1330
// This should be expanded to include all the cases in the issue.

// @skip-test until we fix the issue

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.TreeSet;

public class TreeSetTest {

    public static void main(String[] args) {

        // :: error: (type.argument.type.incompatible)
        TreeSet<@Nullable Integer> ts = new TreeSet<>();

        // This throws a null pointer exception
        ts.add(null);
    }
}
