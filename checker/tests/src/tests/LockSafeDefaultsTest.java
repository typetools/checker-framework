package tests;

import org.checkerframework.framework.test.CheckerFrameworkTest;

import java.io.File;

import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Lock checker when using safe defaults for unchecked source code.
 */
public class LockSafeDefaultsTest extends CheckerFrameworkTest {

    public LockSafeDefaultsTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.lock.LockChecker.class,
                "lock",
                "-AuseDefaultsForUncheckedCode=source",
                "-Anomsgtext"
                );
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"lock-safedefaults"};
    }

}
