package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Optional Checker, which has the {@code @Present} annotation. */
public class OptionalTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create an OptionalTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public OptionalTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.optional.OptionalChecker.class,
                "optional",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"optional", "all-systems"};
    }
}
