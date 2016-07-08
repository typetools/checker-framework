package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

public class FenumTest extends CheckerFrameworkTest {

    public FenumTest(File testFile) {
        super(
                testFile,
                org.checkerframework.checker.fenum.FenumChecker.class,
                "fenum",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"fenum", "all-systems"};
    }
}
