package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

public class RegexTest extends ParameterizedCheckerTest {

    public RegexTest(File testFile) {
        super(testFile, checkers.regex.RegexChecker.class.getName(), "regex",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("regex", "all-systems"); }
}
