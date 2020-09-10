package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkPerFileTest;
import org.junit.runners.Parameterized.Parameters;
import testlib.util.EvenOddChecker;

/** JUnit tests for the Checker Framework, using the {@link EvenOddChecker}. */
public class FrameworkTest extends CheckerFrameworkPerFileTest {

    public FrameworkTest(File testFile) {
        super(testFile, EvenOddChecker.class, "framework", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"framework", "all-systems"};
    }
}
