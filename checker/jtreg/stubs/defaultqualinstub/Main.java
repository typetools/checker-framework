/*
 * @test
 * @summary Test that the DefaultQualifier in an astub works.
 * @library .
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Astubs=defaults.astub pck/Defaults.java -Werror
 */

public class Main {}
