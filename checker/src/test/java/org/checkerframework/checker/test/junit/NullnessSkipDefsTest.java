package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Nullness Checker -- testing {@code -AskipDefs} command-line argument. */
public class NullnessSkipDefsTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessSkipDefsTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessSkipDefsTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-Anomsgtext",
                "-AskipDefs=SkipMe");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-skipdefs"};
    }
}
