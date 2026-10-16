/*
 * @test
 * @summary Test that the stub files get invoked
 * @library .
 * @compile -processor org.checkerframework.checker.nullness.NullnessChecker -Astubs=sample.astub Sample.java  -Werror
 */

public class Sample {
  void test() {
    Object o = null;
    String v = String.valueOf(o);
  }
}
