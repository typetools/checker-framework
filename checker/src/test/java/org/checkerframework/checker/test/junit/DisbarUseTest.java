package org.checkerframework.checker.test.junit;

import org.checkerframework.checker.testchecker.disbaruse.DisbarUseChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

public class DisbarUseTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a DisbarUseTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public DisbarUseTest(List<File> testFiles) {
        super(
                testFiles,
                DisbarUseChecker.class,
                "disbaruse-records",
                "-Anomsgtext",
                "-Astubs=tests/disbaruse-records",
                "-AstubWarnIfNotFound");
    }

    @Parameters
    public static String[] getTestDirs() {
        // Check for JDK 16+ without using a library:
        if (System.getProperty("java.version").matches("^(1[6-9]|[2-9][0-9])")) {
            return new String[] {"disbaruse-records"};
        } else {
            return new String[] {};
        }
    }
}
