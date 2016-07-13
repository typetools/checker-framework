package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness Checker -- testing -AskipDefs command-line argument.
 */
public class NullnessSkipDefsTest extends CheckerFrameworkTest {

    public NullnessSkipDefsTest(File testFile) {
        super(
                testFile,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-Anomsgtext",
                "-AskipDefs=SkipMe");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-skipdefs"};
    }
}
