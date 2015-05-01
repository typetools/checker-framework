package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

public class RegexClassicTest extends ParameterizedCheckerTest {

    public RegexClassicTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.regex.classic.RegexClassicChecker.class,
                "regex_classic",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("regex", "regex_poly", "all-systems");
    }
}
