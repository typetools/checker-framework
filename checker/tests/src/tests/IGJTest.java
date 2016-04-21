package tests;

import org.checkerframework.checker.igj.IGJChecker;
import org.checkerframework.framework.test.CheckerFrameworkTest;

import java.io.File;

import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the IGJ Checker.  More tests are run my OldStyleIGJTest
 * @see tests.OldStyleIGJTest
 */
public class IGJTest extends CheckerFrameworkTest {

    public IGJTest(File testFile) {
        super(testFile,
              IGJChecker.class,
                "igj",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        // TODO: https://github.com/typetools/checker-framework/issues/670
        // expected suppress warnings in the all-systems test,
        // correct any unexpected warnings, and add that directory.
        // Also see https://github.com/typetools/checker-framework/issues/669
        return new String[]{"igj"};
    }
}
