package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

/**
 * JUnit tests for the Interning checker, which tests the Interned annotation.
 */
public class InterningTest extends ParameterizedCheckerTest {

    public InterningTest(File testFile) {
        super(testFile, checkers.interning.InterningChecker.class.getName(),
                "interning", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("interning", "all-systems"); }
}
