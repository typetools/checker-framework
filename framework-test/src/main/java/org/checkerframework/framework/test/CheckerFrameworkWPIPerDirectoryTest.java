package org.checkerframework.framework.test;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.StringVal;
import org.junit.Assert;
import javax.annotation.processing.AbstractProcessor;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A specialized variant of {@link CheckerFrameworkPerDirectoryTest} for testing
 * the Whole Program Inference feature of the Checker Framework, which is tested by
 * running pairs of these tests: a "generation test" to do inference using the {@code -Ainfer} option,
 * and a "validation test" to check that files typecheck after those inferences are taken into
 * account.
 */
public abstract class CheckerFrameworkWPIPerDirectoryTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * The directory where .ajava files are found.
   */
  private static final String INFERENCE_BASE_DIR = "tests/ainfer-testchecker/inference-output/";

  /**
   * The suffix of all ainfer-testchecker ajava files.
   */
  private static final String AINFER_TEST_CHECKER_SUFFIX = "-org.checkerframework.checker.testchecker.ainfer.AinferTestChecker";

  /**
   * The suffix of all Value Checker ajava files.
   */
  private static final String VALUE_CHECKER_SUFFIX = "-org.checkerframework.common.value.ValueChecker";

  /**
   * The number of letters in ".java", used when stripping off the extension from
   * the name of a file.
   */
  private static final int DOT_JAVA_LETTER_COUNT = 5;

  /**
   * If this is a validation test, the class of the corresponding generation test.
   */
  private final @Nullable Class<?> predecessorTest;

  /**
   * Creates a new checker test. Use this constructor when creating a generation test.
   *
   * <p>{@link TestConfigurationBuilder#getDefaultConfigurationBuilder(String, File, String,
   * Iterable, Iterable, List, boolean)} adds additional checker options.
   *
   * @param testFiles      the files containing test code, which will be type-checked
   * @param checker        the class for the checker to use
   * @param testDir        the path to the directory of test inputs
   * @param checkerOptions options to pass to the compiler when running tests
   */
  protected CheckerFrameworkWPIPerDirectoryTest(List<File> testFiles, Class<?
      extends AbstractProcessor> checker, String testDir, String... checkerOptions) {
    this(testFiles, checker, testDir, null, checkerOptions);
  }

  /**
   * Creates a new checker test. Use this constructor when creating a validation test.
   *
   * <p>{@link TestConfigurationBuilder#getDefaultConfigurationBuilder(String, File, String,
   * Iterable, Iterable, List, boolean)} adds additional checker options.
   *
   * @param testFiles        the files containing test code, which will be type-checked
   * @param checker          the class for the checker to use
   * @param testDir          the path to the directory of test inputs
   * @param predecessorTest  the class of the test that must run before this test, if this is the
   *                         second of a pair of tests
   * @param checkerOptions options to pass to the compiler when running tests
   */
  protected CheckerFrameworkWPIPerDirectoryTest(List<File> testFiles, Class<?
      extends AbstractProcessor> checker, String testDir, Class<?> predecessorTest, String... checkerOptions) {
    super(testFiles, checker, testDir, checkerOptions);
    this.predecessorTest = predecessorTest;
  }

  /**
   * Computes the -Aajava argument that corresponds to the test files. This method is
   * necessary because the framework issues a warning if a .ajava file
   * with no corresponding source file is specified.
   *
   * Assumes that ajava files will be in the {@link #INFERENCE_BASE_DIR} directory.
   *
   * @param sourceFiles the list of source files
   * @return the appropriate -Aajava argument
   */
  protected static String ajavaArgFromFiles(List<File> sourceFiles) {
    return annotationArgFromFiles(sourceFiles, ".ajava");
  }

  /**
   * Computes the -Astubs argument that corresponds to the test files. This method is
   * necessary because the framework issues a warning if a .astub file
   * with no corresponding source file is specified.
   *
   * Assumes that astub files will be in the {@link #INFERENCE_BASE_DIR} directory.
   *
   * @param sourceFiles the list of source files
   * @return the appropriate -Astubs argument
   */
  protected static String astubsArgFromFiles(List<File> sourceFiles) {
    return annotationArgFromFiles(sourceFiles, ".astub");
  }

  /**
   * Computes the list of ajava or stub files that correspond to the test files. This method is
   * necessary because the framework issues a warning if a .ajava file or a stub file
   * with no corresponding source file is specified.
   *
   * Assumes that ajava/astub files will be in the {@link #INFERENCE_BASE_DIR} directory.
   *
   * @param sourceFiles the list of source files
   * @param extension the extension to use: either .astub or .ajava
   * @return the appropriate -Aajava or -Astubs argument
   */
  private static String annotationArgFromFiles(List<File> sourceFiles, @StringVal({".astub", ".ajava"}) String extension) {
    String checkerArg = extension.equals(".astub") ? "-Astubs=" : "-Aajava=";
    return checkerArg + sourceFiles.stream()
        .map(f -> annotationFilenameFromSourceFile(f, extension))
        .filter(s -> !s.isEmpty())
        .collect(Collectors.joining(":"));
  }

  /**
   * Generates the correct argument to the -Aajava or -Astubs CF option corresponding to
   * the source file {@code f} and the wpi output type.
   * @param f a java source file
   * @param extension the extension to use: either .astub or .ajava
   * @return the ajava argument for the corresponding ajava files, if there are any
   */
  private static String annotationFilenameFromSourceFile(File f, @StringVal({".astub", ".ajava"}) String extension) {
    String fileBaseName = f.getName().substring(0, f.getName().length() - DOT_JAVA_LETTER_COUNT);
    File ainferTestCheckerFile = new File(INFERENCE_BASE_DIR + fileBaseName + AINFER_TEST_CHECKER_SUFFIX + extension);
    File valueCheckerFile = new File(INFERENCE_BASE_DIR + fileBaseName + VALUE_CHECKER_SUFFIX + extension);
    if (ainferTestCheckerFile.exists() && valueCheckerFile.exists()) {
      return ainferTestCheckerFile + ":" + valueCheckerFile;
    } else if (ainferTestCheckerFile.exists()) {
      return ainferTestCheckerFile.toString();
    } else if (valueCheckerFile.exists()) {
      return valueCheckerFile.toString();
    } else {
      return "";
    }
  }

  @Override
  public void run() {
    // Only run if annotated files have been created.
    // See ainferTest task.
    if (predecessorTest != null &&
        !new File("tests/ainfer-testchecker/annotated/").exists()) {
      throw new RuntimeException(
          predecessorTest + " must be run before this test.");
    }
    super.run();
  }

  /**
   * Do not typecheck any file ending with the given String.
   * Use this routine to avoid typechecking files in the all-systems
   * test suite that are problematic for one typechecker. For example,
   * this routine is useful when running the all-systems tests using WPI,
   * because some all-systems tests have expected errors that become warnings
   * during a WPI run (because of -Awarns) and so must be excluded.
   *
   * This code takes advantage of the mutability of the {@link #testFiles}
   * field.
   *
   * @param endswith a string that the absolute path of the target file that should not
   *                 be typechecked ends with. Usually, this takes the form
   *                 "all-systems/ProblematicFile.java".
   */
  protected void doNotTypecheck(String endswith) {
    int removeIndex = -1;
    for (int i = 0; i < testFiles.size(); i++) {
      File f = testFiles.get(i);
      if (f.getAbsolutePath().endsWith(endswith)) {
        if (removeIndex != -1) {
          Assert.fail("When attempting to exclude a file, found more than one " +
              "match in the test suite. Check the test code and use a more-specific " +
              "removal key. Attempting to exclude: " + endswith);
        }
        removeIndex = i;
      }
    }
    // This test code can run for every subdirectory of the all-systems tests, so there is no
    // guarantee that the file to be excluded will be found.
    if (removeIndex != -1) {
      testFiles.remove(removeIndex);
    }
  }
}
