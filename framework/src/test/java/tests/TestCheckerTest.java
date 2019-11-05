package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.FrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;
import testchecker.TestChecker;

/** */
public class TestCheckerTest extends FrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public TestCheckerTest(List<File> testFiles) {
        super(
                testFiles,
                TestChecker.class,
                "testchecker",
                "-Anomsgtext",
                "-Astubs=tests/testchecker/testchecker.astub");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"testchecker"};
    }
}
