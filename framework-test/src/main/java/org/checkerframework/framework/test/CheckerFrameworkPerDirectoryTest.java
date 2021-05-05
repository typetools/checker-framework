package org.checkerframework.framework.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.AbstractProcessor;
import org.checkerframework.checker.signature.qual.BinaryName;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Compiles all test files in a test directory together. Use {@link CheckerFrameworkPerFileTest} to
 * compile each test file in a test directory individually. A {@link
 * CheckerFrameworkPerDirectoryTest} is faster than an equivalent {@link
 * CheckerFrameworkPerFileTest}, but can only test that processor errors or warnings are issued.
 *
 * <p>To create a {@link CheckerFrameworkPerDirectoryTest}, create a new class that extends this
 * class. The new class must do the following:
 *
 * <ol>
 *   <li>Declare a constructor taking 1 parameter of type {@code java.util.List<java.io.File>}. This
 *       is a list of the files that will be compiled.
 *   <li>Declare the following method:
 *       <pre>{@code @Parameters public static String [] getTestDirs()}</pre>
 *       <p>getTestDir must return an array of directories that exist in the test folder. The
 *       directories can contain more path information (e.g., "myTestDir/moreTests") but note, the
 *       test suite will find all of the Java test files that exists below the listed directories.
 *       It is unnecessary to list child directories of a directory you have already listed.
 * </ol>
 *
 * <pre><code>
 * public class MyTest extends CheckerFrameworkPerDirectoryTest {
 *   /** {@literal @}param testFiles the files containing test code, which will be type-checked *{@literal /}
 *   public MyTest(List{@literal <}File{@literal >} testFiles) {
 *     super(testFiles, MyChecker.class, "", "Anomsgtext");
 *   }
 *  {@literal @}Parameters
 *   public static String [] getTestDirs() {
 *     return new String[]{"all-systems"};
 *   }
 * }
 * </code></pre>
 */
@RunWith(PerDirectorySuite.class)
public abstract class CheckerFrameworkPerDirectoryTest {

  /** The files containing test code, which will be type-checked. */
  protected final List<File> testFiles;

  /** The binary names of the checkers to run. */
  protected final List<@BinaryName String> checkerNames;

  /** The path, relative to currentDir/test to the directory containing test inputs. */
  protected final String testDir;

  /** Extra options to pass to javac when running the checker. */
  protected final List<String> checkerOptions;

  /** Extra entries for the classpath. */
  protected final List<String> classpathExtra;

  /**
   * Creates a new checker test.
   *
   * <p>{@link TestConfigurationBuilder#getDefaultConfigurationBuilder(String, File, String,
   * Iterable, Iterable, List, boolean)} adds additional checker options.
   *
   * @param testFiles the files containing test code, which will be type-checked
   * @param checker the class for the checker to use
   * @param testDir the path to the directory of test inputs
   * @param checkerOptions options to pass to the compiler when running tests
   */
  protected CheckerFrameworkPerDirectoryTest(
      List<File> testFiles,
      Class<? extends AbstractProcessor> checker,
      String testDir,
      String... checkerOptions) {
    this(testFiles, checker, testDir, Collections.emptyList(), checkerOptions);
  }

  /**
   * Creates a new checker test.
   *
   * <p>{@link TestConfigurationBuilder#getDefaultConfigurationBuilder(String, File, String,
   * Iterable, Iterable, List, boolean)} adds additional checker options.
   *
   * @param testFiles the files containing test code, which will be type-checked
   * @param checker the class for the checker to use
   * @param testDir the path to the directory of test inputs
   * @param classpathExtra extra entries for the classpath
   * @param checkerOptions options to pass to the compiler when running tests
   */
  @SuppressWarnings(
      "signature:argument" // for non-array non-primitive class, getName(): @BinaryName
  )
  protected CheckerFrameworkPerDirectoryTest(
      List<File> testFiles,
      Class<? extends AbstractProcessor> checker,
      String testDir,
      List<String> classpathExtra,
      String... checkerOptions) {
    this(
        testFiles,
        Collections.singletonList(checker.getName()),
        testDir,
        classpathExtra,
        checkerOptions);
  }

  /**
   * Creates a new checker test.
   *
   * <p>{@link TestConfigurationBuilder#getDefaultConfigurationBuilder(String, File, String,
   * Iterable, Iterable, List, boolean)} adds additional checker options.
   *
   * @param testFiles the files containing test code, which will be type-checked
   * @param checkerNames the binary names of the checkers to run
   * @param testDir the path to the directory of test inputs
   * @param classpathExtra extra entries for the classpath
   * @param checkerOptions options to pass to the compiler when running tests
   */
  protected CheckerFrameworkPerDirectoryTest(
      List<File> testFiles,
      List<@BinaryName String> checkerNames,
      String testDir,
      List<String> classpathExtra,
      String... checkerOptions) {
    this.testFiles = testFiles;
    this.checkerNames = checkerNames;
    this.testDir = "tests" + File.separator + testDir;
    this.classpathExtra = classpathExtra;
    this.checkerOptions = new ArrayList<>(Arrays.asList(checkerOptions));
    this.checkerOptions.add("-AajavaChecks");
  }

  @Test
  public void run() {
    boolean shouldEmitDebugInfo = TestUtilities.getShouldEmitDebugInfo();
    List<String> customizedOptions = customizeOptions(Collections.unmodifiableList(checkerOptions));
    TestConfiguration config =
        TestConfigurationBuilder.buildDefaultConfiguration(
            testDir,
            testFiles,
            classpathExtra,
            checkerNames,
            customizedOptions,
            shouldEmitDebugInfo);
    TypecheckResult testResult = new TypecheckExecutor().runTest(config);
    TypecheckResult adjustedTestResult = adjustTypecheckResult(testResult);
    TestUtilities.assertTestDidNotFail(adjustedTestResult);
  }

  /**
   * This method is called before issuing assertions about a TypecheckResult. Subclasses can
   * override it to customize behavior.
   *
   * @param testResult a test result to possibly change
   * @return a TypecheckResult to use instead, which may be the unmodified argument
   */
  public TypecheckResult adjustTypecheckResult(TypecheckResult testResult) {
    return testResult;
  }

  /**
   * Override this method if you would like to supply a checker command-line option that depends on
   * the Java files passed to the test. Those files are available in field {@link #testFiles}.
   *
   * <p>If you want to specify the same command-line option for all tests of a particular checker,
   * then pass it to the {@link #CheckerFrameworkPerDirectoryTest} constructor.
   *
   * @param previousOptions the options specified in the constructor of the test previousOptions is
   *     unmodifiable
   * @return a new list of options or the original passed through
   */
  public List<String> customizeOptions(List<String> previousOptions) {
    return previousOptions;
  }
}
