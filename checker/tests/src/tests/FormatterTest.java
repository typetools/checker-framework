package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

public class FormatterTest extends CheckerFrameworkTest {
    public FormatterTest(File testFile) {
        super(
                testFile,
                org.checkerframework.checker.formatter.FormatterChecker.class,
                "formatter",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"formatter", "all-systems"};
    }
}
