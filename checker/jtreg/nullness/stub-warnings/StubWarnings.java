/*
 * @test
 * @summary Test warnings for redundant stub specifications.
 *
 * @compile -XDrawDiagnostics -Xlint:unchecked Binary.java
 * @compile/fail/ref=StubWarnings.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=binary.astub -AstubWarnIfRedundantWithBytecode -AstubWarnIfOverwritesBytecode -Werror StubWarnings.java
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=binary.astub StubWarnings.java
 */
public class StubWarnings {}
