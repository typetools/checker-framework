package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Lower Bound checker.
 */
public class LowerBoundTest extends CheckerFrameworkTest {

    public LowerBoundTest(File testFile) {
        super(
                testFile,
                org.checkerframework.checker.lowerbound.LowerBoundChecker.class,
                "lowerbound",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"lowerbound"};
    }
}
