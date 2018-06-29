package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class I18nTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
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
