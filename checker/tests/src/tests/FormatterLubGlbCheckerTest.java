package tests;

// Test case for issue 691.
// https://github.com/typetools/checker-framework/issues/691
// This exists to just run the FormatterLubGlbChecker

import org.checkerframework.framework.test.CheckerFrameworkTest;

import java.io.File;

import org.junit.runners.Parameterized.Parameters;

import tests.lubglb.FormatterLubGlbChecker;

public class FormatterLubGlbCheckerTest extends CheckerFrameworkTest {

    public FormatterLubGlbCheckerTest(File testFile) {
        super(testFile, FormatterLubGlbChecker.class, "", "-Anomsgtext", "-AcheckPurityAnnotations");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"formatter-lubglb"};
    }

}
