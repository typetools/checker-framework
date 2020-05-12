package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.FrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;
import testaccumulation.TestAccumulationChecker;

/**
 * A test that the accumulation abstract checker is working correctly, using a simple accumulation
 * checker.
 */
public class AccumulationTest extends FrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public AccumulationTest(List<File> testFiles) {
        super(testFiles, TestAccumulationChecker.class, "accumulation", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"accumulation", "all-systems"};
    }
}
