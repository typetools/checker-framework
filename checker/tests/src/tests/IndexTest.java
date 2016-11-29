package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Index Checker. */
public class IndexTest extends CheckerFrameworkPerDirectoryTest {

    public IndexTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.index.IndexChecker.class,
                "index",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"index", "all-systems"};
    }
}
