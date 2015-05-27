package tests;

import org.checkerframework.framework.test.ParameterizedCheckerTest;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness Checker (that uses the Freedom Before Commitment
 * type system for initialization).
 */
public class NullnessAssumeAssertionsAreDisabled extends ParameterizedCheckerTest {

    public NullnessAssumeAssertionsAreDisabled(File testFile) {
        super(testFile,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AassumeAssertionsAreDisabled",
                "-Anomsgtext", "-Xlint:deprecation"
              );
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("nullness-assumeassertions");
    }

}
