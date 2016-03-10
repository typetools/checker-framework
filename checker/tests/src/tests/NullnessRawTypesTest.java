package tests;

import org.checkerframework.framework.test.CheckerFrameworkTest;

import java.io.File;

import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for the Nullness Checker on raw types
 */
public class NullnessRawTypesTest extends CheckerFrameworkTest {

    public NullnessRawTypesTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"nullness-rawtypes"};
    }

}
