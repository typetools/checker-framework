package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

/**
 * JUnit tests for the NonNull checker (that uses the Freedom Before Commitment
 * type system for initialization).
 */
public class NonNullFbcTest extends ParameterizedCheckerTest {

    public NonNullFbcTest(File testFile) {
        super(testFile, checkers.nonnull.NonNullFbcChecker.class.getName(),
                "nonnull", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("nonnull", "nonnull2", "initialization/fbc");
    }

}
