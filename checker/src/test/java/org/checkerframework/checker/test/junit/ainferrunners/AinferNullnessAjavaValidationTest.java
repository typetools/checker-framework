package org.checkerframework.checker.test.junit.ainferrunners;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.framework.test.AinferValidatePerDirectoryTest;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests whole-program type inference with ajava files. This test is the second pass, which ensures
 * that with the ajava files in place, the errors that those annotations remove are no longer
 * issued.
 */
@Category(AinferNullnessAjavaGenerationTest.class)
public class AinferNullnessAjavaValidationTest extends AinferValidatePerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public AinferNullnessAjavaValidationTest(List<File> testFiles) {
    super(
        testFiles,
        NullnessChecker.class,
        "nullness",
        "ainfer-nullness/annotated",
        AinferNullnessAjavaGenerationTest.class,
        ajavaArgFromFiles(testFiles, "nullness"),
        "-Awarns");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"ainfer-nullness/annotated/"};
  }
}
