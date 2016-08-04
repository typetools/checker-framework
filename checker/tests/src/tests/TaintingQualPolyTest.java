package tests;

import java.io.File;
import org.checkerframework.checker.tainting.TaintingChecker;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

public class TaintingQualPolyTest extends CheckerFrameworkTest {

    public TaintingQualPolyTest(File testFile) {
        super(testFile, TaintingChecker.class, "tainting_qual_poly", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"tainting_qual_poly", "all-systems"};
    }
}
