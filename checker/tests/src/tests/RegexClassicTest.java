package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

public class RegexClassicTest extends DefaultCheckerTest {

    public RegexClassicTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.regex.classic.RegexClassicChecker.class,
                "regex_classic",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> getTestFiles() {
        return TestUtilities.findNestedJavaTestFiles("regex", "regex_poly", "all-systems");
    }
}
