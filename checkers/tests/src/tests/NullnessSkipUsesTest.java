package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

/**
 * JUnit tests for the nullness checker -- testing -AskipUses command-line argument.
 */
public class NullnessSkipUsesTest extends ParameterizedCheckerTest {

    public NullnessSkipUsesTest(File testFile) {
        super(testFile, checkers.nonnull.NullnessFbcChecker.class.getName(),
                "nullness", "-Anomsgtext", "-AskipUses=SkipMe");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("nonnull-skipuses"); }

}
