package tests;

// Test case for issue 723.
// https://github.com/typetools/checker-framework/issues/723
// This exists to just run the I18nFormatterLubGlbChecker

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;
import tests.lubglb.I18nFormatterLubGlbChecker;

public class I18nFormatterLubGlbCheckerTest extends CheckerFrameworkTest {

    public I18nFormatterLubGlbCheckerTest(File testFile) {
        super(
                testFile,
                I18nFormatterLubGlbChecker.class,
                "",
                "-Anomsgtext",
                "-AcheckPurityAnnotations");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"i18n-formatter-lubglb"};
    }
}
