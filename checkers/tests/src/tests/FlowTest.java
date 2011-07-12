package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 */
public class FlowTest extends ParameterizedCheckerTest {

    public FlowTest(File testFile) {
        super(testFile, "checkers.util.test.FlowTestChecker", "flow");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("flow"); }
}
