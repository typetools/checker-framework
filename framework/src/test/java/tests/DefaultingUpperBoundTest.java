package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.FrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;
import testlib.defaulting.DefaultingUpperBoundChecker;

/** Created by jburke on 9/29/14. */
public class DefaultingUpperBoundTest extends FrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public DefaultingUpperBoundTest(List<File> testFiles) {
        super(testFiles, DefaultingUpperBoundChecker.class, "defaulting", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"defaulting/upperbound"};
    }
}
