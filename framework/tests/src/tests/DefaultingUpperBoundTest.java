package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Created by jburke on 9/29/14.
 */
public class DefaultingUpperBoundTest extends CheckerFrameworkPerDirectoryTest {

    public DefaultingUpperBoundTest(List<File> testFiles) {
        super(
                testFiles,
                tests.defaulting.DefaultingUpperBoundChecker.class,
                "defaulting",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"defaulting/upperbound"};
    }
}
