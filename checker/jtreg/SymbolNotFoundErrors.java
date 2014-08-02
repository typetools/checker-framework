/*
 * @test
 * @summary Test for expected number of error messages.
 * @compile/fail/ref=SymbolNotFoundErrors.out -XDrawDiagnostics SymbolNotFoundErrors.java
 * @compile/fail/ref=SymbolNotFoundErrors.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker SymbolNotFoundErrors.java
 */
public class SymbolNotFoundErrors {
    // We only expect one error message for the unknown symbol.
    // However, we receive three.
    // http://code.google.com/p/checker-framework/issues/detail?id=94
    CCC f;
}
