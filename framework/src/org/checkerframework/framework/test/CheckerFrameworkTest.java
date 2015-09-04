package org.checkerframework.framework.test;

import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.processing.AbstractProcessor;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.checkerframework.framework.test.TestConfigurationBuilder.buildDefaultConfiguration;

/**
 * To use this class you must do two things:
 * 1) Create exactly 1 constructor in the subclass with exactly 1 argument which is a java.io.File.  This File
 * will be the Java file that is compiled and whose output is verified.
 * 2) Create one of the following 2 public static methods with the annotation with org.junit.runners.Parameterized.Parameters.
 * The method name and signature must match exactly (though you can place @Parameters on the line above the method).
 *
 * {@code @Parameters public static String [] getTestDirs()}
 * getTestDir must return an array of directories that exist in the test folder, e.g.
 * {@code @Parameters public static String [] getTestDirs() { return new String[]{"all-systems", "flow"}}}
 * The directories can contain more path information (e.g.  "myTestDir/moreTests") but note,
 * the testsuite will find all of the java test files that exists below the listed directories.
 * It is unnecessary to list child directories of a directory you have already listed.
 *
 * {@code @Parameters public static List<File> getTestFiles(); }
 * The method returns a List of Java files. There are methods in TestUtilities like findNestedJavaTestFiles to help
 * you construct this List. The TestSuite will then instantiate the subclass once for each file returned by
 * getTestFiles and execute the run method.
 * An example of this method is:
 *
 * {@code @Parameters public static List<File> getTestFiles() { return TestUtilities.findNestedJavaTestFiles("aggregate"); } }
 *
 */
@RunWith(TestSuite.class)
public abstract class CheckerFrameworkTest {

    protected final File testFile;

    /** The fully-qualified class name of the checker to use for tests. */
    protected final String checkerName;

    /** The path, relative to currentDir/test to the directory containing test inputs. */
    protected final String checkerDir;

    /** Extra options to pass to javac when running the checker. */
    protected final List<String> checkerOptions;

    /**
     * Creates a new checker test.
     *
     * @param checker the class for the checker to use
     * @param checkerDir the path to the directory of test inputs
     * @param checkerOptions options to pass to the compiler when running tests
     */
    public CheckerFrameworkTest(File testFile,
                                Class<? extends AbstractProcessor> checker,
                                String checkerDir, String... checkerOptions) {
        this.testFile = testFile;
        this.checkerName = checker.getName();
        this.checkerDir = "tests" + File.separator + checkerDir;
        this.checkerOptions = Arrays.asList(checkerOptions);
    }

    @Test
    public void run() {
        boolean shouldEmitDebugInfo = TestUtilities.getShouldEmitDebugInfo();
        List<String> customizedOptions = customizeOptions(Collections.unmodifiableList(checkerOptions));
        TestConfiguration config = buildDefaultConfiguration(checkerDir, testFile, checkerName, customizedOptions,
                                                             shouldEmitDebugInfo);
        TypecheckResult testResult = new TypecheckExecutor().runTest(config);
        TestUtilities.assertResultsAreValid(testResult);
    }

    /**
     * Override this method if you would like to customize Checker Options per Java file
     * passed to the test.
     * @param previousOptions The options specified in the constructor of the test
     *                        previousOptions is unmodifiable
     * @return A new list of options or the original passed through
     */
    public List<String> customizeOptions(List<String> previousOptions) {
        return previousOptions;
    }

}
