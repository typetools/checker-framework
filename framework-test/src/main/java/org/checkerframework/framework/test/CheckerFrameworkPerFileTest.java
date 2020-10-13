package org.checkerframework.framework.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.AbstractProcessor;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Compiles all test files individually. Use {@link CheckerFrameworkPerDirectoryTest} to compile all
 * files in a test directory together.
 *
 * <p>To use this class you must do two things:
 *
 * <ol>
 *   <li>Create exactly 1 constructor in the subclass with exactly 1 argument of type java.io.File.
 *       This File will be the Java file that is compiled and whose output is verified.
 *   <li>Create one of the following 2 public static methods with the annotation
 *       org.junit.runners.Parameterized.Parameters. The method name and signature must match
 *       exactly.
 *       <ul>
 *         <li>{@code @Parameters public static String [] getTestDirs()}
 *             <p>getTestDir must return an array of directories that exist in the test folder, e.g.
 *             <pre>  @Parameters
 *   public static String [] getTestDirs() {
 *      return new String[]{"all-systems", "flow"};
 *   }</pre>
 *             The directories can contain more path information (e.g., "myTestDir/moreTests") but
 *             note, the test suite will find all of the Java test files that exists below the
 *             listed directories. It is unnecessary to list child directories of a directory you
 *             have already listed.
 *         <li>{@code @Parameters public static List<File> getTestFiles() }
 *             <p>The method returns a List of Java files. There are methods like {@link
 *             TestUtilities#findNestedJavaTestFiles} to help you construct this List. The
 *             PerDirectorySuite will then instantiate the subclass once for each file returned by
 *             getTestFiles and execute the run method. An example of this method is:
 *             <pre>  @Parameters
 *   public static List&lt;File&gt; getTestFiles() {
 *     return TestUtilities.findNestedJavaTestFiles("aggregate");
 *   }</pre>
 *       </ul>
 * </ol>
 */
@RunWith(PerFileSuite.class)
public abstract class CheckerFrameworkPerFileTest {

    /** The file containing test code, which will be type-checked. */
    protected final File testFile;

    /** The checker to use for tests. */
    protected final Class<?> checker;

    /** The path, relative to currentDir/test to the directory containing test inputs. */
    protected final String testDir;

    /** Extra options to pass to javac when running the checker. */
    protected final List<String> checkerOptions;

    /**
     * Creates a new checker test.
     *
     * <p>{@link TestConfigurationBuilder#getDefaultConfigurationBuilder(String, File, String,
     * Iterable, Iterable, List, boolean)} adds additional checker options.
     *
     * @param testFile the file containing test code, which will be type-checked
     * @param checker the class for the checker to use
     * @param testDir the path to the directory of test inputs
     * @param checkerOptions options to pass to the compiler when running tests
     */
    protected CheckerFrameworkPerFileTest(
            File testFile,
            Class<? extends AbstractProcessor> checker,
            String testDir,
            String... checkerOptions) {
        this.testFile = testFile;
        this.checker = checker;
        this.testDir = "tests" + File.separator + testDir;
        this.checkerOptions = new ArrayList<>(Arrays.asList(checkerOptions));
    }

    @Test
    public void run() {
        boolean shouldEmitDebugInfo = TestUtilities.getShouldEmitDebugInfo();
        List<String> customizedOptions =
                customizeOptions(Collections.unmodifiableList(checkerOptions));
        TestConfiguration config =
                TestConfigurationBuilder.buildDefaultConfiguration(
                        testDir, testFile, checker, customizedOptions, shouldEmitDebugInfo);
        TypecheckResult testResult = new TypecheckExecutor().runTest(config);
        TestUtilities.assertTestDidNotFail(testResult);
    }

    /**
     * Override this method if you would like to supply a checker command-line option that depends
     * on the Java file passed to the test. That file name is available in field {@link #testFile}.
     *
     * <p>If you want to specify the same command-line option for all tests of a particular checker,
     * then pass it to the {@link CheckerFrameworkPerFileTest} constructor.
     *
     * @param previousOptions the options specified in the constructor of the test previousOptions
     *     is unmodifiable
     * @return a new list of options or the original passed through
     */
    public List<String> customizeOptions(List<String> previousOptions) {
        return previousOptions;
    }
}
