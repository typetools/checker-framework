package org.checkerframework.checker.test.junit;

import com.google.common.collect.ImmutableList;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.test.TestConfiguration;
import org.checkerframework.framework.test.TestConfigurationBuilder;
import org.checkerframework.framework.test.TestUtilities;
import org.checkerframework.framework.test.TypecheckExecutor;
import org.checkerframework.framework.test.TypecheckResult;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * This test suite exists to demonstrate and keep a record of the unsoundness that occurs when
 * Lombok and the Checker Framework are run in the same invocation of javac.
 */
public class NullnessNoDelombokTest extends CheckerFrameworkPerDirectoryTest {

    private static final ImmutableList<String> ANNOTATION_PROCS =
            ImmutableList.of(
                    "lombok.launch.AnnotationProcessorHider$AnnotationProcessor",
                    "lombok.launch.AnnotationProcessorHider$ClaimingProcessor",
                    org.checkerframework.checker.nullness.NullnessChecker.class.getName());

    public NullnessNoDelombokTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness-nodelombok",
                "-Anomsgtext",
                "-nowarn");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-nodelombok"};
    }

    /**
     * copy-pasted code from {@link CheckerFrameworkPerDirectoryTest#run()}, except that we change
     * the annotation processors to {@link #ANNOTATION_PROCS}
     */
    @Override
    public void run() {
        boolean shouldEmitDebugInfo = TestUtilities.getShouldEmitDebugInfo();
        List<String> customizedOptions =
                customizeOptions(Collections.unmodifiableList(checkerOptions));
        TestConfiguration config =
                TestConfigurationBuilder.buildDefaultConfiguration(
                        testDir,
                        testFiles,
                        classpathExtra,
                        ANNOTATION_PROCS,
                        customizedOptions,
                        shouldEmitDebugInfo);
        TypecheckResult testResult = new TypecheckExecutor().runTest(config);
        TypecheckResult adjustedTestResult = adjustTypecheckResult(testResult);
        TestUtilities.assertTestDidNotFail(adjustedTestResult);
    }
}
