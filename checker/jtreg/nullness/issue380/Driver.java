/*
 * @test
 * @summary Test for Issue 141
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AprintErrorStack Decl.java DA.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AprintErrorStack DB.java
 *
 *
 */
class Driver {}
