package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.index.upperbound.UpperBoundChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Upper Bound checker. */
public class UpperBoundTest extends CheckerFrameworkPerDirectoryTest {

    public UpperBoundTest(List<File> testFiles) {
        super(testFiles, UpperBoundChecker.class, "upperbound", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"upperbound", "all-systems"};
    }
}
