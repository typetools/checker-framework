/*
 * @test
 * @summary Test that --release 8 does not cause a crash.
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.interning.InterningChecker Jdk11Release8.java --release 8
 */

public class Jdk11Release8 {}
