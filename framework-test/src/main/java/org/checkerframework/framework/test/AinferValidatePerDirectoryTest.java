package org.checkerframework.framework.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import org.checkerframework.common.value.qual.StringVal;

/**
 * A specialized variant of {@link CheckerFrameworkPerDirectoryTest} for testing the Whole Program
 * Inference feature of the Checker Framework, which is tested by running pairs of these tests: a
 * "generation test" (of class {@link AinferGeneratePerDirectoryTest}) to do inference using the
 * {@code -Ainfer} option, and a "validation test" (of this class) to check that files typecheck
 * after those inferences are taken into account.
 */
public class AinferValidatePerDirectoryTest extends CheckerFrameworkWPIPerDirectoryTest {

  /** The class of the corresponding generation test. */
  private final Class<? extends AinferGeneratePerDirectoryTest> generationTest;

  /** The directory where inference output files are found. */
  private static final String INFERENCE_BASE_DIR = "tests/ainfer-testchecker/inference-output/";

  /**
   * The number of letters in ".java", used when stripping off the extension from the name of a
   * file.
   */
  private static final int DOT_JAVA_LETTER_COUNT = ".java".length();

  /**
   * Creates a new checker test. Use this constructor when creating a validation test.
   *
   * <p>{@link TestConfigurationBuilder#getDefaultConfigurationBuilder(String, File, String,
   * Iterable, Iterable, List, boolean)} adds additional checker options.
   *
   * @param testFiles the files containing test code, which will be type-checked
   * @param checker the class for the checker to use
   * @param testDir the path to the directory of test inputs
   * @param generationTest the class of the test that must run before this test, if this is the
   *     second of a pair of tests
   * @param checkerOptions options to pass to the compiler when running tests
   */
  protected AinferValidatePerDirectoryTest(
      List<File> testFiles,
      Class<? extends AbstractProcessor> checker,
      String testDir,
      Class<? extends AinferGeneratePerDirectoryTest> generationTest,
      String... checkerOptions) {
    super(testFiles, checker, testDir, checkerOptions);
    this.generationTest = generationTest;
  }

  /**
   * Computes the -Aajava argument that corresponds to the test files. This method is necessary
   * because the framework issues a warning if a .ajava file with no corresponding source file is
   * specified.
   *
   * <p>Assumes that ajava files will be in the {@link #INFERENCE_BASE_DIR} directory.
   *
   * @param sourceFiles the list of source files
   * @return the appropriate -Aajava argument
   */
  protected static String ajavaArgFromFiles(List<File> sourceFiles) {
    return annotationArgFromFiles(sourceFiles, ".ajava");
  }

  /**
   * Computes the -Astubs argument that corresponds to the test files. This method is necessary
   * because the framework issues a warning if a .astub file with no corresponding source file is
   * specified.
   *
   * <p>Assumes that astub files will be in the {@link #INFERENCE_BASE_DIR} directory.
   *
   * @param sourceFiles the list of source files
   * @return the appropriate -Astubs argument
   */
  protected static String astubsArgFromFiles(List<File> sourceFiles) {
    return annotationArgFromFiles(sourceFiles, ".astub");
  }

  /**
   * Computes the list of ajava or stub files that correspond to the test files. This method is
   * necessary because the framework issues a warning if a .ajava file or a stub file with no
   * corresponding source file is specified.
   *
   * <p>Assumes that ajava/astub files will be in the {@link #INFERENCE_BASE_DIR} directory.
   *
   * @param sourceFiles the list of source files
   * @param extension the extension to use: either .astub or .ajava
   * @return the appropriate -Aajava or -Astubs argument
   */
  private static String annotationArgFromFiles(
      List<File> sourceFiles, @StringVal({".astub", ".ajava"}) String extension) {
    String checkerArg = extension.equals(".astub") ? "-Astubs=" : "-Aajava=";
    return checkerArg
        + sourceFiles.stream()
            .map(f -> annotationFilenameFromSourceFile(f, extension))
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining(":"));
  }

  /**
   * Generates the correct argument to the -Aajava or -Astubs CF option corresponding to the source
   * file {@code sourceFile} and the wpi output type.
   *
   * @param sourceFile a java source file
   * @param extension the extension to use: either .astub or .ajava
   * @return the ajava argument for the corresponding ajava files, if there are any
   */
  private static String annotationFilenameFromSourceFile(
      File sourceFile, @StringVal({".astub", ".ajava"}) String extension) {
    String fileBaseName =
        sourceFile.getName().substring(0, sourceFile.getName().length() - DOT_JAVA_LETTER_COUNT);
    StringBuilder sb = new StringBuilder();
    // Find all the annotation files associated with this class name. This approach is necessary
    // because (1) some tests are in packages, which will be included in the annotation file names,
    // and (2) separate astub files are generated for inner classes.
    try (DirectoryStream<Path> dirStream =
        Files.newDirectoryStream(
            Paths.get(INFERENCE_BASE_DIR), "*" + fileBaseName + "{-,$}*" + extension)) {
      dirStream.forEach(f -> sb.append(f).append(":"));
    } catch (IOException ignored) {

    }
    // remove the last ":"
    if (sb.length() > 0) {
      sb.deleteCharAt(sb.length() - 1);
    }
    return sb.toString();
  }

  @Override
  public void run() {
    // Only run if annotated files have been created.
    // See ainferTest task.
    if (generationTest != null && !new File("tests/ainfer-testchecker/annotated/").exists()) {
      throw new RuntimeException(generationTest + " must be run before this test.");
    }
    super.run();
  }
}
