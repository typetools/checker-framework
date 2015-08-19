package tests;

// Test case for issue 343.
// https://github.com/typetools/checker-framework/issues/343
// This exists to just run the NestedAggregateChecker

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class NestedAggregateCheckerTest extends DefaultCheckerTest {

    public NestedAggregateCheckerTest(File testFile) {
        super(testFile, NestedAggregateChecker.class, "", "-Anomsgtext", "-AcheckPurityAnnotations");
    }

    @Parameters
    public static Collection<Object[]> getTestFiles() {
        return TestUtilities.findNestedJavaTestFiles("aggregate", "all-systems");
    }

}
