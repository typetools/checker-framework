package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.defaulting.DefaultingUpperBoundChecker;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** Created by jburke on 9/29/14. */
public class DefaultingUpperBoundTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public DefaultingUpperBoundTest(List<File> testFiles) {
        super(testFiles, DefaultingUpperBoundChecker.class, "defaulting", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"defaulting/upperbound"};
    }
}
