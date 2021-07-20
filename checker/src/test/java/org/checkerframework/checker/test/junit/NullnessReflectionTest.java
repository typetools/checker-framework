package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** JUnit tests for the Nullness checker when reflection resolution is enabled. */
public class NullnessReflectionTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessReflectionTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessReflectionTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AresolveReflection",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-reflection"};
    }
}
