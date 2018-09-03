package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class RegexTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public RegexTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.regex.RegexChecker.class,
                "regex",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"regex", "regex_poly", "all-systems"};
    }
}
