package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

public class I18nFormatterTest extends CheckerFrameworkTest {

    public I18nFormatterTest(File testFile) {
        super(
                testFile,
                org.checkerframework.checker.i18nformatter.I18nFormatterChecker.class,
                "i18n-formatter",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"i18n-formatter", "all-systems"};
    }
}
