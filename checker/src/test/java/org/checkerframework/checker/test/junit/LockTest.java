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
        return new String[] {"lock", "all-systems"};
    }
}
