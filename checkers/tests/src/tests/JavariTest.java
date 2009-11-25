package tests;

import org.junit.runners.Parameterized.Parameters;

import java.util.*;

/**
 * JUnit tests for the Javari annotation checker.
 */
public class JavariTest extends ParameterizedCheckerTest {

    public JavariTest(String testName) {
        super(testName, "checkers.javari.JavariChecker", "javari", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("javari"); }
}
