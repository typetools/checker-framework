/*
 * @test
 * @summary Test -ArequirePrefixInWarningSuppressions
 *
 * @compile/fail/ref=RequireCheckerPrefix.1.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -ArequirePrefixInWarningSuppressions RequireCheckerPrefix.java
 * @compile/fail/ref=RequireCheckerPrefix.2.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker RequireCheckerPrefix.java
 */

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class RequireCheckerPrefix {

    void method(@Nullable Object o) {
        @SuppressWarnings("nullness:assignment.type.incompatible")
        @NonNull Object s = o;
        // "all" is not a valid prefix, so the warning is never suppressed.
        @SuppressWarnings("all:assignment.type.incompatible")
        @NonNull Object t = o;
        @SuppressWarnings("allcheckers:assignment.type.incompatible")
        @NonNull Object u = o;

        @SuppressWarnings("assignment.type.incompatible")
        @NonNull Object p = o;
        // Suppresses the warning if -ArequirePrefixInWarningSuppressions isn't used.
        @SuppressWarnings("all")
        @NonNull Object q = o;
        @SuppressWarnings("allcheckers")
        @NonNull Object w = o;
    }
}
