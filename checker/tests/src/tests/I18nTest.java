package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

public class I18nTest extends CheckerFrameworkTest {

    public I18nTest(File testFile) {
        super(testFile, org.checkerframework.checker.i18n.I18nChecker.class, "i18n", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"i18n", "all-systems"};
    }
}
