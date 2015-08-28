package tests;

import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;

public class RegexQualTest extends CheckerFrameworkTest {

    public RegexQualTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.experimental.regex_qual.RegexCheckerAdapter.class,
                "regex_qual",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"regex", "all-systems"};
    }
}

