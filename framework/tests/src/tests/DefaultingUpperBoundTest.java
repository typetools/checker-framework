package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Created by jburke on 9/29/14.
 */
public class DefaultingUpperBoundTest extends CheckerFrameworkTest {

    public DefaultingUpperBoundTest(File testFile) {
        super(
                testFile,
                tests.defaulting.DefaultingUpperBoundChecker.class,
                "defaulting",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"defaulting/upperbound"};
    }
}
