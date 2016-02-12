package tests;

import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;

/**
 * JUnit tests for the Interning Checker, which tests the Interned annotation.
 */
public class InterningTest extends CheckerFrameworkTest {

    public InterningTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.interning.InterningChecker.class,
                "interning",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"interning", "all-systems"};
    }
}
