package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

public class RegexTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a RegexTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public RegexTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.regex.RegexChecker.class,
                "regex",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"regex", "regex_poly", "all-systems"};
    }
}
