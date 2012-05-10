package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

/**
 * JUnit tests for the Nullness checker -- testing initialization code.
 */
public class NullnessUninitializedTest extends ParameterizedCheckerTest {

    public NullnessUninitializedTest(File testFile) {
        super(testFile, checkers.nullness.NullnessChecker.class.getName(),
                "nullness", "-Anomsgtext", "-Alint=uninitialized");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("nullness-uninit"); }

}
