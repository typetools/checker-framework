package org.checkerframework.checker.test.junit.wpirunners;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.testchecker.wholeprograminference.WholeProgramInferenceTestChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests whole-program type inference with stub files. This test is the second pass, which ensures
 * that with the stubs in place, the errors that those annotations remove are no longer issued.
 */
@Category(WholeProgramInferenceStubsTest.class)
public class WholeProgramInferenceStubsValidationTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public WholeProgramInferenceStubsValidationTest(List<File> testFiles) {
        super(
                testFiles,
                WholeProgramInferenceTestChecker.class,
                "whole-program-inference/annotated",
                "-Anomsgtext",
                "-Astubs=build/whole-program-inference",
                // "-AstubDebug",
                "-AmergeStubsWithSource");
    }

    @Override
    public void run() {
        // Only run if annotated files have been created.
        // See wholeProgramInferenceTests task.
        if (!new File("tests/whole-program-inference/annotated/").exists()) {
            throw new RuntimeException(
                    WholeProgramInferenceStubsTest.class + " must be run before this test.");
        }
        super.run();
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"whole-program-inference/annotated/"};
    }
}
