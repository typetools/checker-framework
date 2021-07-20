package org.checkerframework.checker.test.junit;

import org.checkerframework.checker.tainting.TaintingChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

public class TaintingTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a TaintingTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public TaintingTest(List<File> testFiles) {
        super(testFiles, TaintingChecker.class, "tainting", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"tainting", "all-systems"};
    }
}
