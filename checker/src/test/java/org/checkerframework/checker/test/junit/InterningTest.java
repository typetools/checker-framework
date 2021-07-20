package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** JUnit tests for the Interning Checker, which tests the Interned annotation. */
public class InterningTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create an InterningTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public InterningTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.interning.InterningChecker.class,
                "interning",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"interning", "all-systems"};
    }
}
