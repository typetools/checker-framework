package tests;

import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class RegexTest extends ParameterizedCheckerTest {

    public RegexTest(String testName) {
        super(testName, "checkers.regex.RegexChecker", "regex", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("regex"); }
}
