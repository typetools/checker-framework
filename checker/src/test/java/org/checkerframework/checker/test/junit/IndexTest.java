package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** JUnit tests for the Index Checker. */
public class IndexTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create an IndexTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public IndexTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.index.IndexChecker.class,
                "index",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"index", "all-systems"};
    }
}
