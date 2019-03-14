/*
 * @test
 * @summary Test -ArequirePrefixInWarningSuppressions
 *
 * @compile/fail/ref=RequireCheckerPrefix.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -ArequirePrefixInWarningSuppressions RequireCheckerPrefix.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker RequireCheckerPrefix.java
 */

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class RequireCheckerPrefix {

    void method(@Nullable Object o) {
        @SuppressWarnings("nullness:assignment.type.incompatible")
        @NonNull Object s = o;
        @SuppressWarnings("assignment.type.incompatible")
        @NonNull Object p = o;
    }
}
