package tests;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.Collection;

public class RegexQualTest extends DefaultCheckerTest {

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

