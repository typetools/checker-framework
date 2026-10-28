/*
 * @test
 * @summary Test case for Issue 1585 https://github.com/typetools/checker-framework/issues/1585
 * and for more general Java parse errors in stub files.
 *
 * ParseError.goal and UnknownField.goal contain expected warnings.
 *
 * @compile/ref=UnknownField.goal -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Astubs=UnknownField.astub -Anomsgtext Main.java
 * @compile/ref=ParseError.goal -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Astubs=ParseError.astub -Anomsgtext Main.java
 */

package issue1585;

public class Main {}
