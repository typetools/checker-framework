package tests;

import java.io.File;

import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

/**
 */
public class LockTest extends CheckerFrameworkTest {

    public LockTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.lock.LockChecker.class,
                "lock",
                "-Anomsgtext"
                );
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"lock", "all-systems"};
    }
}
