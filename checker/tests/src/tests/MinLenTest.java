package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Run the Junit tests for the MinLen Checker.
 */
public class MinLenTest extends CheckerFrameworkPerDirectoryTest {
    public MinLenTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.minlen.MinLenChecker.class,
                "minlen",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"minlen"};
    }
}
