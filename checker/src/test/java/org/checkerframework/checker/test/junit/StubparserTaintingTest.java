package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

/** Tests for stub parsing. */
public class StubparserTaintingTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a StubparserTaintingTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public StubparserTaintingTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.tainting.TaintingChecker.class,
                "stubparser-tainting",
                "-Anomsgtext",
                "-AmergeStubsWithSource",
                "-Astubs=tests/stubparser-tainting/TypeParamWithInner.astub");
    }

    @Parameterized.Parameters
    public static String[] getTestDirs() {
        return new String[] {"stubparser-tainting", "all-systems"};
    }
}
