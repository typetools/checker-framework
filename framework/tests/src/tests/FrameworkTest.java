package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

import tests.util.TestChecker;

/**
 * JUnit tests for the Checker Framework, using the {@link TestChecker}.
 */
public class FrameworkTest extends ParameterizedCheckerTest {

    public FrameworkTest(File testFile) {
        super(testFile,
                tests.util.TestChecker.class,
                "framework",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("framework", "all-systems");
    }
}
