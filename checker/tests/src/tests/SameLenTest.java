package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Run the Junit tests for the SameLen Checker. */
public class SameLenTest extends CheckerFrameworkPerDirectoryTest {
    public SameLenTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.samelen.SameLenChecker.class,
                "samelen",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"samelen"};
    }
}
