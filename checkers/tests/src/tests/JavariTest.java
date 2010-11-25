package tests;

import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.*;

/**
 * JUnit tests for the Javari annotation checker.
 */
public class JavariTest extends ParameterizedCheckerTest {

    public JavariTest(File testFile) {
        super(testFile, "checkers.javari.JavariChecker", "javari", "-Anomsgtext", "-Anocheckjdk");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("javari"); }
}
