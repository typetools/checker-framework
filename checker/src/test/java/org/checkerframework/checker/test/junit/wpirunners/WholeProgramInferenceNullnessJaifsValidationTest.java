package org.checkerframework.checker.test.junit.wpirunners;

import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/**
 * Tests whole-program type inference with the aid of .jaif files. This test is the second pass,
 * which ensures that with the annotations inserted, the errors are no longer issued.
 */
@Category(WholeProgramInferenceNullnessJaifsTest.class)
public class WholeProgramInferenceNullnessJaifsValidationTest
        extends CheckerFrameworkPerDirectoryTest {
    /** @param testFiles the files containing test code, which will be type-checked */
    public WholeProgramInferenceNullnessJaifsValidationTest(List<File> testFiles) {
        super(testFiles, NullnessChecker.class, "nullness", "-Anomsgtext");
    }

    @Override
    public void run() {
        // Only run if annotated files have been created.
        // See wholeProgramInferenceTests task.
        if (!new File("tests/wpi-nullness/annotated/").exists()) {
            throw new RuntimeException(
                    WholeProgramInferenceNullnessJaifsTest.class
                            + " must be run before this test.");
        }
        super.run();
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"wpi-nullness/annotated/"};
    }
}
