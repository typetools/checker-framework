package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness Checker (that uses the Freedom Before Commitment
 * type system for initialization).
 */
public class NullnessAssumeAssertionsAreDisabled extends CheckerFrameworkTest {

    public NullnessAssumeAssertionsAreDisabled(File testFile) {
        super(
                testFile,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AassumeAssertionsAreDisabled",
                "-Anomsgtext",
                "-Xlint:deprecation");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-assumeassertions"};
    }
}
