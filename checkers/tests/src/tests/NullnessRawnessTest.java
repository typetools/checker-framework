package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.nullness.AbstractNullnessChecker;
import checkers.util.test.ParameterizedCheckerTest;

/**
 * JUnit tests for the Nullness checker (that uses the rawness type system for
 * initialization).
 */
public class NullnessRawnessTest extends ParameterizedCheckerTest {

    public NullnessRawnessTest(File testFile) {
        // TODO: remove arrays:forbidnonnullcomponents option once it's no
        // longer needed.
        super(testFile, checkers.nullness.NullnessRawnessChecker.class.getName(),
                "nullness", "-Anomsgtext", "-Xlint:deprecation",
                "-Alint=arrays:forbidnonnullcomponents,"
                        + AbstractNullnessChecker.LINT_REDUNDANTNULLCOMPARISON);
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("nullness");
    }

}
