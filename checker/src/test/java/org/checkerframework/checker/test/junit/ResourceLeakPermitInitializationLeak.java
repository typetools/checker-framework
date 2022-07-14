package org.checkerframework.checker.test.junit;

import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** Tests for the Resource Leak Checker. */
public class ResourceLeakPermitInitializationLeak extends CheckerFrameworkPerDirectoryTest {
    public ResourceLeakPermitInitializationLeak(List<File> testFiles) {
        super(
                testFiles,
                ResourceLeakChecker.class,
                "resourceleak-permitinitializationleak",
                "-ApermitInitializationLeak",
                "-AwarnUnneededSuppressions",
                "-encoding",
                "UTF-8");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"resourceleak-permitinitializationleak"};
    }
}
