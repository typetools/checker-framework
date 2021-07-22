package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

public class MustCallTest extends CheckerFrameworkPerDirectoryTest {
    public MustCallTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.mustcall.MustCallChecker.class,
                "mustcall",
                "-Anomsgtext",
                // "-AstubDebug");
                "-nowarn");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"mustcall"};
    }
}
