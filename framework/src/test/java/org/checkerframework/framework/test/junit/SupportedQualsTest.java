package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.supportedquals.SupportedQualsChecker;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

public class SupportedQualsTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public SupportedQualsTest(List<File> testFiles) {
        super(testFiles, SupportedQualsChecker.class, "simple", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"simple"};
    }
}
