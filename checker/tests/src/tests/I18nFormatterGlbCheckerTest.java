package tests;

// Test case for issue 723.
// https://github.com/typetools/checker-framework/issues/723
// This exists to just run the I18nFormatterGlbChecker

import org.checkerframework.framework.test.CheckerFrameworkTest;

import java.io.File;

import org.junit.runners.Parameterized.Parameters;

import tests.glb.I18nFormatterGlbChecker;

public class I18nFormatterGlbCheckerTest extends CheckerFrameworkTest {

    public I18nFormatterGlbCheckerTest(File testFile) {
        super(testFile, I18nFormatterGlbChecker.class, "", "-Anomsgtext", "-AcheckPurityAnnotations");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"i18n-formatter-glb"};
    }

}
