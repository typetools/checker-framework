/*
 * @test
 * @summary Test that the stub files get invoked
 * @library .
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Astubs=sample.astub Sample.java  -AstubWarnIfNotFound -Werror
 */

public class Sample {
  void test() {
    Object o = null;
    String v = String.valueOf(o);
  }
}
