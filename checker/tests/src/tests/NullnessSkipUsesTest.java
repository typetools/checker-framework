package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness Checker -- testing -AskipUses command-line argument.
 */
public class NullnessSkipUsesTest extends ParameterizedCheckerTest {

    public NullnessSkipUsesTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-Anomsgtext", "-AskipUses=SkipMe");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("nullness-skipuses");
    }

}
