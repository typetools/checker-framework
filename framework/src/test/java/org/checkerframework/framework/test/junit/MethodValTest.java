package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Tests the MethodVal Checker. */
public class MethodValTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public MethodValTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.common.reflection.MethodValChecker.class,
                "methodval",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"methodval"};
    }
}
