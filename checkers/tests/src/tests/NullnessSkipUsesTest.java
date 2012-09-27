package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

/**
 * JUnit tests for the Nullness checker -- testing -AskipUses command-line argument.
 */
public class NullnessSkipUsesTest extends ParameterizedCheckerTest {

    public NullnessSkipUsesTest(File testFile) {
        super(testFile, checkers.nullness.NullnessChecker.class.getName(),
                "nullness", "-Anomsgtext", "-AskipUses=SkipMe", "-Alint=advancedchecks");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("nullness-skipuses"); }

}
