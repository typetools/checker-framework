package tests;

import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness checker.
 */
public class NullnessTest extends ParameterizedCheckerTest {

    public NullnessTest(String testName) {
        super(testName, "checkers.nullness.NullnessChecker", "nullness", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("nullness"); }

}
