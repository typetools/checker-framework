package tests;

// Test case for issue 723.
// https://github.com/typetools/checker-framework/issues/723
// This exists to just run the I18nFormatterLubGlbChecker

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;
import testlib.lubglb.I18nFormatterLubGlbChecker;

public class I18nFormatterLubGlbCheckerTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public I18nFormatterLubGlbCheckerTest(List<File> testFiles) {
        super(
                testFiles,
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
