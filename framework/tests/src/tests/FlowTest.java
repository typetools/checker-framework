package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

/**
 */
public class FlowTest extends ParameterizedCheckerTest {

    public FlowTest(File testFile) {
        super(testFile,
                tests.util.FlowTestChecker.class,
                "flow",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("flow", "all-systems");
    }
}
