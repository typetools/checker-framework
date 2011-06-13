package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Interning checker, which tests the Interned annotation.
 */
public class InterningTest extends ParameterizedCheckerTest {

    public InterningTest(File testFile) {
        super(testFile, "checkers.interning.InterningChecker", "interning", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("interning"); }
}
