/*
 * @test
 * @summary Test that a stub file can have no package, but have an annotation on the class.
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Astubs=NoPackage.astub Driver2.java -Werror
 */

public class Driver2 {
  void test() {}
}
