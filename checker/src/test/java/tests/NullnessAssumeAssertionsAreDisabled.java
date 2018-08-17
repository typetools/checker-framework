package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness Checker (that uses the Freedom Before Commitment type system for
 * initialization).
 */
public class NullnessAssumeAssertionsAreDisabled extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public NullnessAssumeAssertionsAreDisabled(List<File> testFiles) {
        super(
                testFiles,
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
