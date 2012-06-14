package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

/**
 * JUnit tests for the Nullness checker -- testing -AskipDefs command-line argument.
 */
public class NullnessSkipDefsTest extends ParameterizedCheckerTest {

    public NullnessSkipDefsTest(File testFile) {
        super(testFile, checkers.nullness.NullnessChecker.class.getName(),
                "nullness", "-Anomsgtext", "-AskipDefs=SkipMe", "-Alint=advancedchecks");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("nullness-skipdefs"); }

}
