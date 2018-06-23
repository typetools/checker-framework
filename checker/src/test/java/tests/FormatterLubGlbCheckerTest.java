package tests;

// Test case for issue 691.
// https://github.com/typetools/checker-framework/issues/691
// This exists to just run the FormatterLubGlbChecker

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;
import testlib.lubglb.FormatterLubGlbChecker;

public class FormatterLubGlbCheckerTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public FormatterLubGlbCheckerTest(List<File> testFiles) {
        super(
                testFiles,
                FormatterLubGlbChecker.class,
                "",
                "-Anomsgtext",
                "-AcheckPurityAnnotations");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"formatter-lubglb"};
    }
}
