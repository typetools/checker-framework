package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness Checker -- testing -AskipUses command-line argument.
 */
public class NullnessSkipUsesTest extends DefaultCheckerTest {

    public NullnessSkipUsesTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-Anomsgtext", "-AskipUses=SkipMe");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"nullness-skipuses"};
    }

}
