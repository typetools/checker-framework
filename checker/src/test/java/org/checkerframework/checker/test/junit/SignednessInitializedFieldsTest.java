package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SignednessInitializedFieldsTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a SignednessInitializedFieldsTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public SignednessInitializedFieldsTest(List<File> testFiles) {
        super(
                testFiles,
                Arrays.asList(
                        "org.checkerframework.common.initializedfields.InitializedFieldsChecker",
                        "org.checkerframework.checker.signedness.SignednessChecker"),
                "signedness-initialized-fields",
                Collections.emptyList(), // classpathextra
                "-Anomsgtext",
                "-AsuppressWarnings=type.checking.not.run");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"signedness-initialized-fields", "all-systems"};
    }
}
