package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

public class I18nFormatterTest extends DefaultCheckerTest {

    public I18nFormatterTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.i18nformatter.I18nFormatterChecker.class,
                "i18n-formatter",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"i18n-formatter", "all-systems"};
    }
}
