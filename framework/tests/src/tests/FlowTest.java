package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

/**
 */
public class FlowTest extends DefaultCheckerTest {

    public FlowTest(File testFile) {
        super(testFile,
                tests.util.FlowTestChecker.class,
                "flow",
                "-Anomsgtext");
    }

    @Parameters
    public static String [] getTestDirs() {
        return new String[]{"flow", "all-systems"};
    }
}
