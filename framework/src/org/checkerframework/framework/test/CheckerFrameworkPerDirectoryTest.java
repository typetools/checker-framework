package org.checkerframework.framework.test;

import static org.checkerframework.framework.test.TestConfigurationBuilder.buildDefaultConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.AbstractProcessor;
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

    protected final List<File> testFiles;

    /** The fully-qualified class name of the checker to use for tests. */
    protected final String checkerName;

    /** The path, relative to currentDir/test to the directory containing test inputs. */
    protected final String testDir;

    /** Extra options to pass to javac when running the checker. */
    protected final List<String> checkerOptions;

    /**
     * Creates a new checker test.
     *
     * <p>{@link TestConfigurationBuilder#getDefaultConfigurationBuilder(String, File, String,
     * Iterable, Iterable, List, boolean)} adds additional checker options such as
     * -AprintErrorStack.
     *
     * @param checker the class for the checker to use
     * @param testDir the path to the directory of test inputs
     * @param checkerOptions options to pass to the compiler when running tests
     */
    public CheckerFrameworkPerDirectoryTest(
            List<File> testFiles,
            Class<? extends AbstractProcessor> checker,
            String testDir,
            String... checkerOptions) {
        this.testFiles = testFiles;
        this.checkerName = checker.getName();
        this.testDir = "tests" + File.separator + testDir;
        this.checkerOptions = Arrays.asList(checkerOptions);
    }

    @Test
    public void run() {
        boolean shouldEmitDebugInfo = TestUtilities.getShouldEmitDebugInfo();
        List<String> customizedOptions =
                customizeOptions(Collections.unmodifiableList(checkerOptions));
        TestConfiguration config =
                buildDefaultConfiguration(
                        testDir,
                        testFiles,
                        Collections.singleton(checkerName),
                        customizedOptions,
                        shouldEmitDebugInfo);
        TypecheckResult testResult = new TypecheckExecutor().runTest(config);
        TestUtilities.assertResultsAreValid(testResult);
    }

    /**
     * Override this method if you would like to supply a checker command-line option that depends
     * on the Java files passed to the test. Those files are available in field {@link #testFiles}.
     *
     * <p>If you want to specify the same command-line option for all tests of a particular checker,
     * then pass it to the {@link #CheckerFrameworkPerDirectoryTest} constructor.
     *
     * @param previousOptions the options specified in the constructor of the test previousOptions
     *     is unmodifiable
     * @return a new list of options or the original passed through
     */
    public List<String> customizeOptions(List<String> previousOptions) {
        return previousOptions;
    }
}
