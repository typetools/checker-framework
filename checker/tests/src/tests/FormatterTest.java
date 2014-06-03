package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

public class FormatterTest extends ParameterizedCheckerTest {
    public FormatterTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.formatter.FormatterChecker.class,
                "formatter",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("formatter", "all-systems");
    }
}
