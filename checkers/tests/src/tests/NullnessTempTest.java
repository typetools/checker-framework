package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

/**
 * JUnit tests for the Nullness checker.
 */
public class NullnessTempTest extends ParameterizedCheckerTest {

    public NullnessTempTest(File testFile) {
        super(testFile, checkers.nullness.NullnessChecker.class.getName(),
                "nullness", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("nullness-temp"); }

}
