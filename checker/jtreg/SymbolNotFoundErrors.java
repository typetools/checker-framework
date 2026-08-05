/*
 * @test
 * @summary Test for expected number of error messages.
 * @compile/fail/ref=SymbolNotFoundErrors.goal -XDrawDiagnostics SymbolNotFoundErrors.java
 * @compile/fail/ref=SymbolNotFoundErrors2.goal -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker SymbolNotFoundErrors.java
 */
public class SymbolNotFoundErrors {
  CCC f;
}
