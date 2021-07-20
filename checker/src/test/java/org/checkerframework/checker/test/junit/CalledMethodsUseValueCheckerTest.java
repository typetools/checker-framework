package org.checkerframework.checker.test.junit;

import org.checkerframework.checker.calledmethods.CalledMethodsChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.List;

public class CalledMethodsUseValueCheckerTest extends CheckerFrameworkPerDirectoryTest {
    public CalledMethodsUseValueCheckerTest(List<File> testFiles) {
        super(
                testFiles,
                CalledMethodsChecker.class,
                "calledmethods-usevaluechecker",
                "-Anomsgtext",
                "-AuseValueChecker",
                "-nowarn");
    }

    @Parameterized.Parameters
    public static String[] getTestDirs() {
        return new String[] {"calledmethods-usevaluechecker"};
    }
}
