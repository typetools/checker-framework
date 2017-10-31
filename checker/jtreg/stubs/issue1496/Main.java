/*
 * @test
 * @summary Test case for Issue 1496 https://github.com/typetools/checker-framework/issues/1496
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Astubs=ClassAnnotation.astub Main.java -Werror -AstubWarnIfNotFound
 */

package issue1496;

public class Main {}
