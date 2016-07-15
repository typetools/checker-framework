package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness Checker -- testing -AskipUses command-line argument.
 */
public class NullnessSkipUsesTest extends CheckerFrameworkPerDirectoryTest {

    public NullnessSkipUsesTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-Anomsgtext",
                "-AskipUses=SkipMe");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-skipuses"};
    }
}
