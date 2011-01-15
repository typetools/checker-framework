package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness checker.
 */
public class NullnessTest extends ParameterizedCheckerTest {

    public NullnessTest(File testFile) {
        super(testFile, "checkers.nullness.NullnessChecker", "nullness", "-Anomsgtext", "-Anocheckjdk");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("nullness"); }

}
