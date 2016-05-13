package tests;

// Test case for issue 691.
// https://github.com/typetools/checker-framework/issues/691
// This exists to just run the FormatterGlbChecker

import org.checkerframework.framework.test.CheckerFrameworkTest;

import java.io.File;

import org.junit.runners.Parameterized.Parameters;

import tests.glb.FormatterGlbChecker;

public class FormatterGlbCheckerTest extends CheckerFrameworkTest {

    public FormatterGlbCheckerTest(File testFile) {
        super(testFile, glb.FormatterGlbChecker.class, "", "-Anomsgtext", "-AcheckPurityAnnotations");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"formatter-glb"};
    }

}
