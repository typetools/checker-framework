/*
 * @test
 * @summary Test the DotCfgVisualizer doesn't crash for varargs method invocation.
 *
 * @compile -Aflowdotdir=. -Averbosecfg  -processor org.checkerframework.common.value.ValueChecker DotCfgVisualizerForVarargsTest.java
 */

public class DotCfgVisualizerForVarargsTest {

  public DotCfgVisualizerForVarargsTest(Object... objs) {}

  public static void method(Object... objs) {}

  public void call() {
    new DotCfgVisualizerForVarargsTest();
    new DotCfgVisualizerForVarargsTest(1, 2);

    method();
    method("", null);
  }
}
