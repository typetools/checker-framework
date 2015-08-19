package tests;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.Collection;

public class RegexTest extends DefaultCheckerTest {

    public RegexTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.regex.RegexChecker.class,
                "regex",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> getTestFiles() {
        return TestUtilities.findNestedJavaTestFiles("regex", "regex_poly", "regex_qual_poly", "all-systems");
    }
}
