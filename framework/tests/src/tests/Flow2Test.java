package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for the flow-sensitive part of the framework. These tests complement
 * the tests of {@link FlowTest} and have been written when the org.checkerframework.dataflow
 * analysis has been completely rewritten.
 *
 * @author Stefan Heule
 *
 */
public class Flow2Test extends CheckerFrameworkTest {

    public Flow2Test(File testFile) {
        super(
                testFile,
                tests.util.FlowTestChecker.class,
                "flow",
                "-Anomsgtext",
                "-AcheckPurityAnnotations");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"flow2"};
    }
}
