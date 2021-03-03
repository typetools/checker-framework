package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class InitializedFieldsValueTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a InitializedFieldsValueTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public InitializedFieldsValueTest(List<File> testFiles) {
        super(
                testFiles,
                Arrays.asList(
                        "org.checkerframework.common.initializedfields.InitializedFieldsChecker",
                        "org.checkerframework.common.value.ValueChecker"),
                "initialized-fields-value",
                Collections.emptyList(), // classpathextra
                "-Anomsgtext",
                "-AnoWarnOnTypeCheckingHalt");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"initialized-fields-value", "all-systems"};
    }
}
