package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.util.FlowTestChecker;
import org.junit.runners.Parameterized.Parameters;

/** */
public class FlowTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public FlowTest(List<File> testFiles) {
        super(testFiles, FlowTestChecker.class, "flow", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"flow", "all-systems"};
    }
}
