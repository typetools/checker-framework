package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

public class FormatterUncheckedDefaultsTest extends CheckerFrameworkPerDirectoryTest {
    /**
     * Create a FormatterUncheckedDefaultsTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public FormatterUncheckedDefaultsTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.formatter.FormatterChecker.class,
                "formatter",
                "-Anomsgtext",
                "-AuseConservativeDefaultsForUncheckedCode=-source,bytecode");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"formatter-unchecked-defaults"};
    }
}
