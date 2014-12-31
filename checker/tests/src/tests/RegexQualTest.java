package tests;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.Collection;

public class RegexQualTest extends ParameterizedCheckerTest {

    public RegexQualTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.experimental.regex_qual.RegexCheckerAdapter.class,
                "regex_qual",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("regex", "all-systems");
    }
}

