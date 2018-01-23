package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.linear.LinearChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class LinearTest extends CheckerFrameworkPerDirectoryTest {

    public LinearTest(List<File> testFiles) {
        super(testFiles, LinearChecker.class, "linear", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"linear"};
    }
}
