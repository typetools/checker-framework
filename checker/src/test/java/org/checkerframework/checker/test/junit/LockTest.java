package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

public class LockTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a LockTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public LockTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.lock.LockChecker.class,
                "lock",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        // Check for JDK 16+ without using a library:
        if (System.getProperty("java.version").matches("^(1[6-9]|[2-9][0-9])\\..*")) {
            return new String[] {"lock", "lock-records", "all-systems"};
        } else {
            return new String[] {"lock", "all-systems"};
        }
    }
}
