package tests;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 * Created by jburke on 9/29/14.
 */
public class DefaultingLowerBoundTest extends DefaultCheckerTest {

    public DefaultingLowerBoundTest(File testFile) {
        super(testFile,
              tests.defaulting.DefaultingLowerBoundChecker.class,
              "defaulting",
              "-Anomsgtext"
        );
    }

    @Parameters
    public static Collection<Object[]> getTestFiles() {
        return TestUtilities.findNestedJavaTestFiles("defaulting/lowerbound");
    }
}
