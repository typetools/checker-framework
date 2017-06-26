/*
 * @test
 * @summary Test the DOTCFGVisualizer doesn't crash for varargs method invocation.
 *
 * @compile -Aflowdotdir=. -Averbosecfg  -processor org.checkerframework.common.value.ValueChecker DOTCFGVisualizerForVarargsTest.java -AprintErrorStack
 */

class DOTCFGVisualizerForVarargsTest {

    public DOTCFGVisualizerForVarargsTest(Object... objs) {}

    public static void method(Object... objs) {}

    public void call() {
        new DOTCFGVisualizerForVarargsTest();
        new DOTCFGVisualizerForVarargsTest(1, 2);

        method();
        method("", null);
    }
}
