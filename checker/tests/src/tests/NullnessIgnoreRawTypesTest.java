package tests;

import org.checkerframework.framework.test.CheckerFrameworkTest;

import java.io.File;

import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for the Nullness Checker on raw types using the ignoreRawTypeArguments option
 */
public class NullnessIgnoreRawTypesTest extends CheckerFrameworkTest {

    public NullnessIgnoreRawTypesTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-Anomsgtext",
                "-AignoreRawTypeArguments");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"nullness-rawtypes-ignore"};
    }

}
