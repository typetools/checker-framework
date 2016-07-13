package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Created by jburke on 9/29/14.
 */
public class DefaultingLowerBoundTest extends CheckerFrameworkTest {

    public DefaultingLowerBoundTest(File testFile) {
        super(
                testFile,
                tests.defaulting.DefaultingLowerBoundChecker.class,
                "defaulting",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"defaulting/lowerbound"};
    }
}
