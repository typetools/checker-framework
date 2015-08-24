package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

/**
 */
public class LubGlbTest extends DefaultCheckerTest {

    public LubGlbTest(File testFile) {
        super(testFile,
                lubglb.LubGlbChecker.class,
                "lubglb",
                "-Anomsgtext");
    }

    @Parameters
    public static String [] getTestDirs() {
        return new String[]{"lubglb"};
    }
}