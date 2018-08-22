package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class FormatterUncheckedDefaultsTest extends CheckerFrameworkPerDirectoryTest {
    /** @param testFiles the files containing test code, which will be type-checked */
    public FormatterUncheckedDefaultsTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.formatter.FormatterChecker.class,
                "formatter",
                "-Anomsgtext",
                "-AuseDefaultsForUncheckedCode=-source,bytecode");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"formatter-unchecked-defaults"};
    }
}
