package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

public class FormatterTest extends DefaultCheckerTest {
    public FormatterTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.formatter.FormatterChecker.class,
                "formatter",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> getTestFiles() {
        return TestUtilities.findNestedJavaTestFiles("formatter", "all-systems");
    }
}
