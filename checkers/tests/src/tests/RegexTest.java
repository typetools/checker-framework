package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class RegexTest extends ParameterizedCheckerTest {

    public RegexTest(File testFile) {
        super(testFile, "checkers.regex.RegexChecker", "regex", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("regex"); }
}
