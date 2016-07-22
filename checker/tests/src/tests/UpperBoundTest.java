package tests;

import org.checkerframework.framework.test.CheckerFrameworkTest;
import java.io.File;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Upper Bound checker.
 */
public class UpperBoundTest extends CheckerFrameworkTest {

    public UpperBoundTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.upperbound.UpperBoundChecker.class,
                "upperbound",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"upperbound"};
    }

}
