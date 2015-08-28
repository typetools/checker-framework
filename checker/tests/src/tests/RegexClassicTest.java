package tests;

import java.io.File;

import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

public class RegexClassicTest extends CheckerFrameworkTest {

    public RegexClassicTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.regex.classic.RegexClassicChecker.class,
                "regex_classic",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"regex", "regex_poly", "all-systems"};
    }
}
