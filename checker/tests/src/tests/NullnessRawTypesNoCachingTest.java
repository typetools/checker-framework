package tests;

import org.checkerframework.framework.test.CheckerFrameworkTest;

import java.io.File;

import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for the Nullness Checker on raw types with caching turned off.
 */
public class NullnessRawTypesNoCachingTest extends CheckerFrameworkTest {

    public NullnessRawTypesNoCachingTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-Anomsgtext",
              "-AatfDoNotCache" ,"-AatfDoNotReadCache");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"nullness-rawtypes-nocaching"};
    }

}
