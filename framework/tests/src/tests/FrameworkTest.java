package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;
import tests.util.TestChecker;

/**
 * JUnit tests for the Checker Framework, using the {@link TestChecker}.
 */
public class FrameworkTest extends CheckerFrameworkTest {

    public FrameworkTest(File testFile) {
        super(testFile, tests.util.TestChecker.class, "framework", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"framework", "all-systems"};
    }
}
