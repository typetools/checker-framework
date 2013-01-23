package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.nonnull.AbstractNonNullChecker;
import checkers.util.test.ParameterizedCheckerTest;

/**
 * JUnit tests for the NonNull checker.
 */
public class NonNullTempTest extends ParameterizedCheckerTest {

    public NonNullTempTest(File testFile) {
        // TODO: remove arrays:forbidnonnullcomponents option once it's no
        // longer needed.
        super(testFile, checkers.nonnull.NonNullFbcChecker.class.getName(),
                "nonnull", "-Anomsgtext",
                "-Alint=arrays:forbidnonnullcomponents,"
                        + AbstractNonNullChecker.LINT_STRICTNULLCOMPARISON);
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("nonnull-temp"); }

}
