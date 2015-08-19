package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Interning Checker, which tests the Interned annotation.
 */
public class InterningTest extends DefaultCheckerTest {

    public InterningTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.interning.InterningChecker.class,
                "interning",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> getTestFiles() {
        return TestUtilities.findNestedJavaTestFiles("interning", "all-systems");
    }
}
