package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;
import testlib.defaulting.DefaultingLowerBoundChecker;

/** Created by jburke on 9/29/14. */
public class DefaultingLowerBoundTest extends CheckerFrameworkPerDirectoryTest {

    public DefaultingLowerBoundTest(List<File> testFiles) {
        super(testFiles, DefaultingLowerBoundChecker.class, "defaulting", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"defaulting/lowerbound"};
    }
}
