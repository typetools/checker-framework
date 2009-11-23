package tests;

import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Interning checker, which tests the Interned annotation.
 */
public class InterningTest extends ParameterizedCheckerTest {

    public InterningTest(String testName) {
        super(testName, "checkers.interning.InterningChecker", "interning", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("interning"); }
}
