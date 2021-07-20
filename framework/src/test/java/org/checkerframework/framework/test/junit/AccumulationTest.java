package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.testaccumulation.TestAccumulationChecker;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/**
 * A test that the accumulation abstract checker is working correctly, using a simple accumulation
 * checker.
 */
public class AccumulationTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public AccumulationTest(List<File> testFiles) {
        super(
                testFiles,
                TestAccumulationChecker.class,
                "accumulation",
                "-Anomsgtext",
                "-encoding",
                "UTF-8");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"accumulation", "all-systems"};
    }
}
