package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** JUnit tests for the Nullness Checker -- testing {@code -AskipUses} command-line argument. */
public class NullnessSkipUsesTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessSkipUsesTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessSkipUsesTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-Anomsgtext",
                "-AskipUses=SkipMe");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-skipuses"};
    }
}
