/*
 * @test
 * @summary Test for expected number of error messages.
 * @compile/fail/ref=SymbolNotFoundErrors.out -XDrawDiagnostics SymbolNotFoundErrors.java
 * @compile/fail/ref=SymbolNotFoundErrors2.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker SymbolNotFoundErrors.java
 */
public class SymbolNotFoundErrors {
  CCC f;
}
