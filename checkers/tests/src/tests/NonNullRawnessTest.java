package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

/**
 * JUnit tests for the NonNull checker (that uses the rawness type system for
 * initialization).
 */
public class NonNullRawnessTest extends ParameterizedCheckerTest {

    public NonNullRawnessTest(File testFile) {
        super(testFile, checkers.nonnull.NonNullRawnessChecker.class.getName(),
                "nonnull", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("nonnull", "nonnull2", "initialization/rawness");
    }

}
