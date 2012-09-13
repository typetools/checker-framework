package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

/**
 * JUnit tests for the NonNull checker -- testing initialization code.
 */
public class NonNullUninitializedTest extends ParameterizedCheckerTest {

    // TODO: same test for NonNullRawnessChecker?

    public NonNullUninitializedTest(File testFile) {
        super(testFile, checkers.nonnull.NonNullFbcChecker.class.getName(),
                "nonnull", "-Anomsgtext", "-Alint=uninitialized");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("nonnull-uninit"); }

}
