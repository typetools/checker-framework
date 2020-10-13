package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class SignednessTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a SignednessTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public SignednessTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.signedness.SignednessChecker.class,
                "signedness",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"signedness", "all-systems"};
    }
}
