package org.checkerframework.checker.test.junit.ainferrunners;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.testchecker.ainfer.AinferTestChecker;
import org.checkerframework.framework.test.AinferValidatePerDirectoryTest;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests whole-program type inference with ajava files. This test is the second pass, which ensures
 * that with the ajava files in place, the errors that those annotations remove are no longer
 * issued.
 */
@Category(AinferTestCheckerAjavaGenerationTest.class)
public class AinferTestCheckerAjavaValidationTest extends AinferValidatePerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public AinferTestCheckerAjavaValidationTest(List<File> testFiles) {
    super(
        testFiles,
        AinferTestChecker.class,
        "testchecker",
        "ainfer-testchecker/annotated",
        AinferTestCheckerAjavaGenerationTest.class,
        ajavaArgFromFiles(testFiles, "testchecker"),
        "-AcheckPurityAnnotations",
        "-Awarns");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"ainfer-testchecker/annotated/"};
  }
}
