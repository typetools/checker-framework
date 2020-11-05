package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.tainting.TaintingChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for stub parsing. */
public class StubParserTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a StubParserTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public StubParserTest(List<File> testFiles) {
        super(
                testFiles,
                TaintingChecker.class,
                "stubparser",
                "-Anomsgtext",
                "-AmergeStubsWithSource",
                "-Astubs=tests/stubparser/TypeParamWithInner.astub");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"stubparser", "all-systems"};
    }
}
