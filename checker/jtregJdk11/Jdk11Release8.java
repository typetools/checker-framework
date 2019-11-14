/*
 * @test
 * @summary Test that --release 8 does not cause a crash.
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker Jdk11Release8.java --release 8
 */

class Jdk11Release8 {}
