package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.defaulting.DefaultingLowerBoundChecker;
import org.junit.runners.Parameterized.Parameters;

/** Created by jburke on 9/29/14. */
public class DefaultingLowerBoundTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public DefaultingLowerBoundTest(List<File> testFiles) {
        super(testFiles, DefaultingLowerBoundChecker.class, "defaulting", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"defaulting/lowerbound"};
    }
}
