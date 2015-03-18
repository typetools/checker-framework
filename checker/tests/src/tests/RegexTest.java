package tests;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.Collection;

public class RegexTest extends ParameterizedCheckerTest {

    public RegexTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.regex.RegexChecker.class,
                "regex",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("regex", "regex_poly", "regex_qual_poly", "all-systems");
    }
}
