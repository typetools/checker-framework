import org.checkerframework.checker.nullness.qual.*;

/*
 * @test
 * @summary Test case for Issue 820 https://github.com/typetools/checker-framework/issues/820
 *
 * @compile/fail/ref=ErrorAnonymousClass.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AprintErrorStack -Anomsgtext Error.java AnonymousClass.java
 * @compile/fail/ref=AnonymousClass.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AprintErrorStack -Anomsgtext AnonymousClass.java
 *
 */

class Error {
    @NonNull Object o = null;
}
