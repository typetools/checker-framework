package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class FormatterTest extends CheckerFrameworkPerDirectoryTest {
    public FormatterTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.formatter.FormatterChecker.class,
                "formatter",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"formatter", "all-systems"};
    }
}
