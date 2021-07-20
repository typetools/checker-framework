package org.checkerframework.checker.test.junit;

import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** Tests -AparseAllJdk option. */
public class ParseAllJdkTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a ParseAllJdkTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public ParseAllJdkTest(List<File> testFiles) {
        super(testFiles, NullnessChecker.class, "parse-all-jdk", "-AparseAllJdk");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"parse-all-jdk"};
    }
}
