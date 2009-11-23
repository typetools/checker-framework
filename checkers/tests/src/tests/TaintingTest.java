package tests;

import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class TaintingTest extends ParameterizedCheckerTest {

    public TaintingTest(String testName) {
        super(testName, "checkers.tainting.TaintingChecker", "tainting", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("tainting"); }
}
