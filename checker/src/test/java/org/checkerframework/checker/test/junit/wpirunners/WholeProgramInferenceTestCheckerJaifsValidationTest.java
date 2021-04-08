package org.checkerframework.checker.test.junit.wpirunners;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.testchecker.wholeprograminference.WholeProgramInferenceTestChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests whole-program type inference with the aid of .jaif files. This test is the second pass,
 * which ensures that with the annotations inserted, the errors are no longer issued.
 */
@Category(WholeProgramInferenceTestCheckerJaifsTest.class)
public class WholeProgramInferenceTestCheckerJaifsValidationTest
    extends CheckerFrameworkPerDirectoryTest {
  /** @param testFiles the files containing test code, which will be type-checked */
  public WholeProgramInferenceTestCheckerJaifsValidationTest(List<File> testFiles) {
    super(
        testFiles,
        WholeProgramInferenceTestChecker.class,
        "wpi-testchecker/non-annotated",
        "-Anomsgtext",
        "-Awarns");
  }

  @Override
  public void run() {
    // Only run if annotated files have been created.
    // See wholeProgramInferenceTests task.
    if (!new File("tests/wpi-testchecker/annotated/").exists()) {
      throw new RuntimeException(
          WholeProgramInferenceTestCheckerJaifsTest.class + " must be run before this test.");
    }
    super.run();
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"wpi-testchecker/annotated/"};
  }
}
