/*
 * @test
 * @summary Ensure that an invalid annotation doesn't crash the
 * Checker Framework.
 *
 * @compile/fail/ref=errorAB.goal -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker ClassA.java ClassB.java
 * @compile/fail/ref=errorBA.goal -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker ClassB.java ClassA.java
 */
public class Main {}
