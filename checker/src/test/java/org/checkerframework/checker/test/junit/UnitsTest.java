package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class UnitsTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a UnitsTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public UnitsTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.units.UnitsChecker.class,
                "units",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"units", "all-systems"};
    }
}
