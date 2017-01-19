package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.index.minlen.MinLenChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Run the Junit tests for the MinLen Checker. */
public class MinLenTest extends CheckerFrameworkPerDirectoryTest {
    public MinLenTest(List<File> testFiles) {
        super(testFiles, MinLenChecker.class, "minlen", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        // MinLen Checker is run as a sub-checker by the Upper and Lower Bound Checkers.
        // Since both checkers are runn on all-systems and in order to save time, don't
        // run the MinLen Checker on them by itself.
        return new String[] {"minlen"};
    }
}
