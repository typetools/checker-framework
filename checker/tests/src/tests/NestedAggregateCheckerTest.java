package tests;

// Test case for issue 343.
// https://github.com/typetools/checker-framework/issues/343
// This exists to just run the NestedAggregateChecker

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;
import testlib.NestedAggregateChecker;

public class NestedAggregateCheckerTest extends CheckerFrameworkPerDirectoryTest {

    public NestedAggregateCheckerTest(List<File> testFiles) {
        super(
                testFiles,
                NestedAggregateChecker.class,
                "",
                "-Anomsgtext",
                "-AcheckPurityAnnotations");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"aggregate", "all-systems"};
    }
}
