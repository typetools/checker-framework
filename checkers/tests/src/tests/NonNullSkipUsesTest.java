package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

/**
 * JUnit tests for the NonNull Checker -- testing -AskipUses command-line argument.
 */
public class NonNullSkipUsesTest extends ParameterizedCheckerTest {

    public NonNullSkipUsesTest(File testFile) {
        super(testFile, checkers.nonnull.NonNullFbcChecker.class.getName(),
                "nonnull", "-Anomsgtext", "-AskipUses=SkipMe");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("nonnull-skipuses"); }

}
