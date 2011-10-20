package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Javari annotation checker.
 */
public class JavariTest extends ParameterizedCheckerTest {

    public JavariTest(File testFile) {
        super(testFile, checkers.javari.JavariChecker.class.getCanonicalName(),
                "javari", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("javari", "all-systems"); }
}
