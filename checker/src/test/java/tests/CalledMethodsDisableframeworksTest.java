package tests;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.calledmethods.CalledMethodsChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.test.TestConfiguration;
import org.checkerframework.framework.test.TestConfigurationBuilder;
import org.checkerframework.framework.test.TestUtilities;
import org.checkerframework.framework.test.TypecheckExecutor;
import org.checkerframework.framework.test.TypecheckResult;
import org.junit.runners.Parameterized.Parameters;

public class CalledMethodsDisableframeworksTest extends CheckerFrameworkPerDirectoryTest {

    private static final List<String> ANNOTATION_PROCS =
            Arrays.asList(
                    "com.google.auto.value.extension.memoized.processor.MemoizedValidator",
                    "com.google.auto.value.processor.AutoAnnotationProcessor",
                    "com.google.auto.value.processor.AutoOneOfProcessor",
                    "com.google.auto.value.processor.AutoValueBuilderProcessor",
                    "com.google.auto.value.processor.AutoValueProcessor",
                    CalledMethodsChecker.class.getName());

    public CalledMethodsDisableframeworksTest(List<File> testFiles) {
        super(
                testFiles,
                CalledMethodsChecker.class,
                "calledmethods-disableframeworks",
                "-Anomsgtext",
                "-AdisableFrameworkSupports=AutoValue,Lombok",
                // The next option is so that we can run the EC2 tests under this configuration.
                "-ACalledMethodsChecker_useValueChecker",
                "-nowarn");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"calledmethods-disableframeworks", "calledmethods-cve"};
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
                        ANNOTATION_PROCS,
                        customizedOptions,
                        shouldEmitDebugInfo);
        TypecheckResult testResult = new TypecheckExecutor().runTest(config);
        TestUtilities.assertResultsAreValid(testResult);
    }
}
