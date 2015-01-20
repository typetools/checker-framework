package tests;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.Collection;

public class RegexQualPolyTest extends ParameterizedCheckerTest {

    public RegexQualPolyTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.experimental.regex_qual_poly.RegexCheckerAdapter.class,
                "regex_qual_poly",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("regex", "regex_poly", "regex_qual_poly", "all-systems");
    }
}
