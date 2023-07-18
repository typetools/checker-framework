package org.checkerframework.checker.test.junit.ainferrunners;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.testchecker.ainfer.AinferTestChecker;
import org.checkerframework.framework.test.AinferValidatePerDirectoryTest;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests whole-program type inference with the aid of .jaif files. This test is the second pass,
 * which ensures that with the annotations inserted, the errors are no longer issued.
 */
@Category(AinferTestCheckerJaifsGenerationTest.class)
public class AinferTestCheckerJaifsValidationTest extends AinferValidatePerDirectoryTest {
  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public AinferTestCheckerJaifsValidationTest(List<File> testFiles) {
    super(
        testFiles,
        AinferTestChecker.class,
        "testchecker",
        "ainfer-testchecker/non-annotated",
        AinferTestCheckerJaifsGenerationTest.class,
        "-Awarns",
        "-AskipDefs=TestPure");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"ainfer-testchecker/annotated/"};
  }
}
