package tests;

// Test case for issue 343.
// https://code.google.com/p/checker-framework/issues/detail?id=343
// This exists to just run the NestedAggregateChecker

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.Collection;

public class NestedAggregateCheckerTest extends ParameterizedCheckerTest {

    public NestedAggregateCheckerTest(File testFile) {
        super(testFile, NestedAggregateChecker.class, "", "-Anomsgtext", "-AcheckPurityAnnotations");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("aggregate", "all-systems");
    }

}
