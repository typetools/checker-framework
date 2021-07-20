package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.testaccumulation.TestAccumulationNoReturnsReceiverChecker;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/**
 * A test that the accumulation abstract checker is working correctly, using a simple accumulation
 * checker without a returns-receiver analysis.
 */
public class AccumulationNoReturnsReceiverTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public AccumulationNoReturnsReceiverTest(List<File> testFiles) {
        super(
                testFiles,
                TestAccumulationNoReturnsReceiverChecker.class,
                "accumulation-norr",
                "-Anomsgtext",
                "-encoding",
                "UTF-8");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"accumulation-norr", "all-systems"};
    }
}
