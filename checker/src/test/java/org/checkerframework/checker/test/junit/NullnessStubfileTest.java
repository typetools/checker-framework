package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

public class NullnessStubfileTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessStubfileTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessStubfileTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                // Cannot use -AstubWarnIfNotFound because file junit-assertions.astub is always
                // read, but
                // JUnit 5 might not be on the classpath.  This is unfortunate.
                // "-AstubWarnIfNotFound",
                "-Astubs="
                        + String.join(
                                ":",
                                "tests/nullness-stubfile/stubfile1.astub",
                                "tests/nullness-stubfile/stubfile2.astub",
                                "tests/nullness-stubfile/requireNonNull.astub"));
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-stubfile"};
    }
}
