import org.checkerframework.checker.nullness.qual.NonNull;

/*
 * @test
 * @summary Test case for Issue 820 https://github.com/typetools/checker-framework/issues/820
 *
 * @compile/fail/ref=ErrorAnonymousClass.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext Error.java AnonymousClass.java
 * @compile/fail/ref=AnonymousClass.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext AnonymousClass.java
 *
 */

public class Error {
    @NonNull Object o = null;
}
