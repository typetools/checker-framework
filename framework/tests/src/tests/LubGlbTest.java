package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** */
public class LubGlbTest extends CheckerFrameworkPerDirectoryTest {

    public LubGlbTest(List<File> testFiles) {
        super(testFiles, lubglb.LubGlbChecker.class, "lubglb", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"lubglb"};
    }
}
