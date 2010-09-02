package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness checker.
 */
public class NullnessUninitializedTest extends ParameterizedCheckerTest {

    public NullnessUninitializedTest(File testFile) {
        super(testFile, "checkers.nullness.NullnessChecker", "nullness", "-Anomsgtext", "-Alint=uninitialized");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("nullness/uninit"); }

}
