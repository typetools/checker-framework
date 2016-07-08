package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

/**
 */
public class FlowTest extends CheckerFrameworkTest {

    public FlowTest(File testFile) {
        super(testFile, tests.util.FlowTestChecker.class, "flow", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"flow", "all-systems"};
    }
}
