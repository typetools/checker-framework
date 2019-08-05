package org.checkerframework.framework.test;

import java.io.File;
import java.util.List;
import javax.annotation.processing.AbstractProcessor;

/**
 * The same as {@link org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest} except
 * the annotated jdk is not required.
 */
public abstract class FrameworkPerDirectoryTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Creates a new framework test.
     *
     * <p>{@link TestConfigurationBuilder#getDefaultConfigurationBuilder(String, File, String,
     * Iterable, Iterable, List, boolean)} adds additional checker options.
     *
     * <p>These tests do not require the annotated jdk.
     *
     * @param testFiles the files containing test code, which will be type-checked
     * @param checker the class for the checker to use
     * @param testDir the path to the directory of test inputs
     * @param checkerOptions options to pass to the compiler when running tests
     */
    public FrameworkPerDirectoryTest(
            List<File> testFiles,
            Class<? extends AbstractProcessor> checker,
            String testDir,
            String... checkerOptions) {
        super(testFiles, checker, testDir, checkerOptions);
    }
}
