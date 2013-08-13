package tests;

import checkers.util.test.ParameterizedCheckerTest;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness Checker -- testing -AskipDefs command-line argument.
 */
public class NullnessSkipDefsTest extends ParameterizedCheckerTest {

    public NullnessSkipDefsTest(File testFile) {
        super(testFile,
                checkers.nullness.NullnessChecker.class,
                "nullness",
                "-Anomsgtext", "-AskipDefs=SkipMe");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("nullness-skipdefs");
    }

}
