package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.tainting.TaintingChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class TaintingQualPolyTest extends CheckerFrameworkPerDirectoryTest {

    public TaintingQualPolyTest(List<File> testFiles) {
        super(testFiles, TaintingChecker.class, "tainting_qual_poly", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"tainting_qual_poly", "all-systems"};
    }
}
