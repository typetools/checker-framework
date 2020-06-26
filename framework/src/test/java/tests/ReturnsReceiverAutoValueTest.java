package tests;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.Collections;
import java.util.List;
import org.checkerframework.framework.test.*;
import org.junit.runners.Parameterized.Parameters;

/** tests the returns receiver checker's AutoValue integration. */
public class ReturnsReceiverAutoValueTest extends CheckerFrameworkPerDirectoryTest {

    private static final ImmutableList<String> ANNOTATION_PROCS =
            ImmutableList.of(
                    "com.google.auto.value.extension.memoized.processor.MemoizedValidator",
                    "com.google.auto.value.processor.AutoAnnotationProcessor",
                    "com.google.auto.value.processor.AutoOneOfProcessor",
                    "com.google.auto.value.processor.AutoValueBuilderProcessor",
                    "com.google.auto.value.processor.AutoValueProcessor",
                    org.checkerframework.common.returnsreceiver.ReturnsReceiverChecker.class
                            .getName());

    public ReturnsReceiverAutoValueTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.common.returnsreceiver.ReturnsReceiverChecker.class,
                "basic",
                "-Anomsgtext",
                "-nowarn");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"returnsreceiverautovalue"};
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
