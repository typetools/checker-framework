package tests;

import checkers.util.test.ParameterizedCheckerTest;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class FormatterTest extends ParameterizedCheckerTest {
    public FormatterTest(File testFile) {
        super(testFile,
                checkers.formatter.FormatterChecker.class,
                "formatter",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("formatter", "all-systems");
    }
}
