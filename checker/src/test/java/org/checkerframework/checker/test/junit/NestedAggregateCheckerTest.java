package org.checkerframework.checker.test.junit;

// Test case for issue 343.
// https://github.com/typetools/checker-framework/issues/343
// This exists to just run the NestedAggregateChecker

import org.checkerframework.checker.testchecker.NestedAggregateChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

public class NestedAggregateCheckerTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NestedAggregateCheckerTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
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
