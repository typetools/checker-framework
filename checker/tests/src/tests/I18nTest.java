package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class I18nTest extends CheckerFrameworkPerDirectoryTest {

    public I18nTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.i18n.I18nChecker.class,
                "i18n",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"i18n", "all-systems"};
    }
}
