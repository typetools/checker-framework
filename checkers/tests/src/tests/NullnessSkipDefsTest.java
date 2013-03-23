package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

/**
 * JUnit tests for the Nullness Checker -- testing -AskipDefs command-line argument.
 */
public class NullnessSkipDefsTest extends ParameterizedCheckerTest {

    public NullnessSkipDefsTest(File testFile) {
        super(testFile, checkers.nonnull.NullnessFbcChecker.class.getName(),
                "nullness", "-Anomsgtext", "-AskipDefs=SkipMe");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("nonnull-skipdefs"); }

}
