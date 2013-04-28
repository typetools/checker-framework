package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.nullness.AbstractNullnessChecker;
import checkers.util.test.ParameterizedCheckerTest;

/**
 * JUnit tests for the nullness checker.
 */
public class NullnessTempTest extends ParameterizedCheckerTest {

    public NullnessTempTest(File testFile) {
        // TODO: remove arrays:forbidnonnullcomponents option once it's no
        // longer needed.
        super(testFile, checkers.nullness.NullnessChecker.class.getName(),
                "nullness", "-Anomsgtext",
                "-Alint=arrays:forbidnonnullcomponents,"
                        + AbstractNullnessChecker.LINT_REDUNDANTNULLCOMPARISON);
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("nullness-temp"); }

}
