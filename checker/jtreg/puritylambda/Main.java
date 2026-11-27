/*
 * @test
 * @summary Test that a purity error about a lambda's body names the methods whose purity
 * annotations require the purity that the body lacks.
 *
 * @compile/fail/ref=LambdaPurityRequiredBy.goal -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker -AcheckPurityAnnotations LambdaPurityRequiredBy.java
 */

public class Main {}
