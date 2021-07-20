package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

public class FenumTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a FenumTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public FenumTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.fenum.FenumChecker.class,
                "fenum",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"fenum", "all-systems"};
    }
}
