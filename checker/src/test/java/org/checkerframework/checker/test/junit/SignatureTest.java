package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

public class SignatureTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a SignatureTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public SignatureTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.signature.SignatureChecker.class,
                "signature",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"signature", "all-systems"};
    }
}
