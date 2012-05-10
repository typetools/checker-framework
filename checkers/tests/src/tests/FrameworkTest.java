package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import tests.util.TestChecker;

import checkers.util.test.ParameterizedCheckerTest;

/**
 * JUnit tests for the Checker Framework, using the {@link TestChecker}.
 */
public class FrameworkTest extends ParameterizedCheckerTest {

    public FrameworkTest(File testFile) {
        super(testFile, tests.util.TestChecker.class.getName(),
                "framework", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("framework", "all-systems"); }
}
