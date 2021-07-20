package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** JUnit tests for the Lock checker when using safe defaults for unchecked source code. */
public class LockSafeDefaultsTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a LockSafeDefaultsTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public LockSafeDefaultsTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.lock.LockChecker.class,
                "lock",
                "-AuseConservativeDefaultsForUncheckedCode=source",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"lock-safedefaults"};
    }
}
