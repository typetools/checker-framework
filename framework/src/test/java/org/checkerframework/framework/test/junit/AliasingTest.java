package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class AliasingTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public AliasingTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.common.aliasing.AliasingChecker.class,
                "aliasing",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"aliasing", "all-systems"};
    }
}
