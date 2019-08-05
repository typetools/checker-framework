/*
 * @test
 * @summary Test case for Issue 824 https://github.com/typetools/checker-framework/issues/824
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext NoStubFirst.java NoStubSecond.java
 * @compile -XDrawDiagnostics -Xlint:unchecked ../issue824lib/First.java
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext Second.java -Astubs=First.astub
 */

import org.checkerframework.checker.nullness.qual.Nullable;

public class Second {
    public static void one(
            First.Supplier<Integer> supplier, First.Callable<@Nullable Object> callable) {
        First.method(supplier, callable);
    }

    public static void two(First.Supplier<Integer> supplier, First.Callable<Object> callable) {
        First.method(supplier, callable);
    }
}
