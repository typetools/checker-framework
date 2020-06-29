package org.checkerframework.framework.test;

import java.io.File;
import java.util.List;
import javax.annotation.processing.AbstractProcessor;

/**
 * The same as {@link org.checkerframework.framework.test.CheckerFrameworkPerFileTest}, but does not
 * use the annotated JDK (because the {@code annotated-jdk} resource is under {@code checker/}, not
 * {@code framework/}).
 */
public abstract class FrameworkPerFileTest extends CheckerFrameworkPerFileTest {
    /**
     * Creates a new framework test.
     *
     * <p>{@link TestConfigurationBuilder#getDefaultConfigurationBuilder(String, File, String,
     * Iterable, Iterable, List, boolean)} adds additional checker options.
     *
     * <p>These tests do not use the annotated JDK.
     *
     * @param testFile the file containing test code, which will be type-checked
     * @param checker the class for the checker to use
     * @param testDir the path to the directory of test inputs
     * @param checkerOptions options to pass to the compiler when running tests
     */
    protected FrameworkPerFileTest(
            File testFile,
            Class<? extends AbstractProcessor> checker,
            String testDir,
            String... checkerOptions) {
        super(testFile, checker, testDir, checkerOptions);
    }
}
