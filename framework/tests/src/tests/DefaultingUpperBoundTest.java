package tests;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 * Created by jburke on 9/29/14.
 */
public class DefaultingUpperBoundTest extends DefaultCheckerTest {

    public DefaultingUpperBoundTest(File testFile) {
        super(testFile,
                tests.defaulting.DefaultingUpperBoundChecker.class,
                "defaulting",
                "-Anomsgtext"
        );
    }

    @Parameters
    public static String [] getTestDirs() {
        return new String[]{"defaulting/upperbound"};
    }
}
