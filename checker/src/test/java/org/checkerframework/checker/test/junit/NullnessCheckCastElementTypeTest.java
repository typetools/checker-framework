package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Nullness checker when checkCastElementType is used. */
public class NullnessCheckCastElementTypeTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessCheckCastElementTypeTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessCheckCastElementTypeTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AcheckCastElementType",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-checkcastelementtype"};
    }
}
