package tests;

// Test case for issue 343.
// https://github.com/typetools/checker-framework/issues/343
// This exists to just run the NestedAggregateChecker

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

public class NestedAggregateCheckerTest extends CheckerFrameworkTest {

    public NestedAggregateCheckerTest(File testFile) {
        super(
                testFile,
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
