/*
 * @test
 * @summary Test case for Issue 1585 https://github.com/typetools/checker-framework/issues/1585
 * and for more general Java parse errors in stub files.
 *
 * ParseError.out and UnknownField.out contain expected warnings.
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Astubs=UnknownField.astub Main.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Astubs=ParseError.astub Main.java
 */

package issue1585;

public class Main {}
