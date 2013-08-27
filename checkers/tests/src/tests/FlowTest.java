package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

/**
 */
public class FlowTest extends ParameterizedCheckerTest {

    public FlowTest(File testFile) {
        super(testFile, tests.util.FlowTestChecker.class.getName(),
                "flow", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("flow", "all-systems"); }
}
