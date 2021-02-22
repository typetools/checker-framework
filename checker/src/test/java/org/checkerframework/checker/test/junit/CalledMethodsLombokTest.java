package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.calledmethods.CalledMethodsChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Test that the Called Methods Checker's support for Lombok works correctly. */
public class CalledMethodsLombokTest extends CheckerFrameworkPerDirectoryTest {
    public CalledMethodsLombokTest(List<File> testFiles) {
        super(
                testFiles,
                CalledMethodsChecker.class,
                "calledmethods-delomboked",
                "-Anomsgtext",
                "-nowarn",
                "-AsuppressWarnings=type.anno.before.modifier");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"calledmethods-delomboked"};
    }
}
