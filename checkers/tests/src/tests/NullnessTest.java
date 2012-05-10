package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

/**
 * JUnit tests for the Nullness checker.
 */
public class NullnessTest extends ParameterizedCheckerTest {

    public NullnessTest(File testFile) {
        // TODO: remove advancedchecks option once its no longer needed
        super(testFile, checkers.nullness.NullnessChecker.class.getName(),
                "nullness", "-Anomsgtext", "-Alint=uninitialized",
                "-Alint=advancedchecks");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("nullness", "all-systems"); }

}
