package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.nonnull.AbstractNullnessChecker;
import checkers.util.test.ParameterizedCheckerTest;

/**
 * JUnit tests for the NonNull checker.
 */
public class NonNullTempTest extends ParameterizedCheckerTest {

    public NonNullTempTest(File testFile) {
        // TODO: remove arrays:forbidnonnullcomponents option once it's no
        // longer needed.
        super(testFile, checkers.nonnull.NullnessFbcChecker.class.getName(),
                "nonnull", "-Anomsgtext",
                "-Alint=arrays:forbidnonnullcomponents,"
                        + AbstractNullnessChecker.LINT_REDUNDANTNULLCOMPARISON);
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("nonnull-temp"); }

}
