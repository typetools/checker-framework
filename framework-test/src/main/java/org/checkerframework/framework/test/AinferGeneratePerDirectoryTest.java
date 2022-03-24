package org.checkerframework.framework.test;

import java.io.File;
import java.util.List;
import javax.annotation.processing.AbstractProcessor;

/**
 * A specialized variant of {@link CheckerFrameworkPerDirectoryTest} for testing the Whole Program
 * Inference feature of the Checker Framework, which is tested by running pairs of these tests: a
 * "generation test" (of this class) to do inference using the {@code -Ainfer} option, and a
 * "validation test" (of class {@link AinferValidatePerDirectoryTest}) to check that files typecheck
 * after those inferences are taken into account.
 */
public abstract class AinferGeneratePerDirectoryTest extends CheckerFrameworkWPIPerDirectoryTest {
  /**
   * Creates a new checker test. Use this constructor when creating a generation test.
   *
   * <p>{@link TestConfigurationBuilder#getDefaultConfigurationBuilder(String, File, String,
   * Iterable, Iterable, List, boolean)} adds additional checker options.
   *
   * @param testFiles the files containing test code, which will be type-checked
   * @param checker the class for the checker to use
   * @param testDir the path to the directory of test inputs
   * @param checkerOptions options to pass to the compiler when running tests
   */
  protected AinferGeneratePerDirectoryTest(
      List<File> testFiles,
      Class<? extends AbstractProcessor> checker,
      String testDir,
      String... checkerOptions) {
    super(testFiles, checker, testDir, checkerOptions);
  }
}
