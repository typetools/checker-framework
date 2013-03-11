package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

/**
 * JUnit tests for the NonNull Checker -- testing -AskipDefs command-line argument.
 */
public class NonNullSkipDefsTest extends ParameterizedCheckerTest {

    public NonNullSkipDefsTest(File testFile) {
        super(testFile, checkers.nonnull.NonNullFbcChecker.class.getName(),
                "nonnull", "-Anomsgtext", "-AskipDefs=SkipMe");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("nonnull-skipdefs"); }

}
