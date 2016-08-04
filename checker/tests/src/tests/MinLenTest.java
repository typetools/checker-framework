package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Run the Junit tests for the MinLen Checker.
 */
public class MinLenTest extends CheckerFrameworkTest {
    public MinLenTest(File testFile) {
        super(
                testFile,
                org.checkerframework.checker.minlen.MinLenChecker.class,
                "minlen",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"minlen"};
    }
}
