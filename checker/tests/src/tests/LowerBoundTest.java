package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Lower Bound checker.
 */
public class LowerBoundTest extends CheckerFrameworkPerDirectoryTest {

    public LowerBoundTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.lowerbound.LowerBoundChecker.class,
                "lowerbound",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"lowerbound"};
    }
}
