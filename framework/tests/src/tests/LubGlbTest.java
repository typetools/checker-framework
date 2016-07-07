package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

/**
 */
public class LubGlbTest extends CheckerFrameworkTest {

    public LubGlbTest(File testFile) {
        super(testFile, lubglb.LubGlbChecker.class, "lubglb", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"lubglb"};
    }
}
