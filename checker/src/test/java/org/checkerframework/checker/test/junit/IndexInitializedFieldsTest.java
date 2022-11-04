package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** JUnit tests for the Index Checker when running together with the InitializedFields Checker. */
public class IndexInitializedFieldsTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create an IndexTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public IndexInitializedFieldsTest(List<File> testFiles) {
        super(
                testFiles,
                Arrays.asList(
                        "org.checkerframework.checker.index.IndexChecker",
                        "org.checkerframework.common.initializedfields.InitializedFieldsChecker"),
                "index-initializedfields",
                Collections.emptyList(),
                "-Aajava=tests/index-initializedfields/input-annotation-files/");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"index-initializedfields"};
    }
}
