package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.tainting.TaintingChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class TaintingTest extends CheckerFrameworkPerDirectoryTest {

    public TaintingTest(List<File> testFiles) {
        super(testFiles, TaintingChecker.class, "tainting", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"tainting", "all-systems"};
    }
}
