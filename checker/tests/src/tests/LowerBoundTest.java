package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.index.lowerbound.LowerBoundChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Lower Bound checker. */
public class LowerBoundTest extends CheckerFrameworkPerDirectoryTest {

    public LowerBoundTest(List<File> testFiles) {
        super(testFiles, LowerBoundChecker.class, "lowerbound", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"lowerbound", "all-systems"};
    }
}
