package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.calledmethods.CalledMethodsChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

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
