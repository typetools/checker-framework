package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

public class FormatterTest extends ParameterizedCheckerTest {
    public FormatterTest(File testFile) {
        super(testFile, checkers.formatter.FormatterChecker.class.getName(), "formatter", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("formatter", "all-systems");
    }
}
