package tests;

import static org.checkerframework.framework.test.TestConfigurationBuilder.buildDefaultConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.checkerframework.framework.test.*;
import org.junit.runners.Parameterized.Parameters;

/** Test case for Object Construction Checker's AutoValue support. */
public class ObjectConstructionAutoValueTest extends CheckerFrameworkPerDirectoryTest {

    private static final List<String> ANNOTATION_PROCS =
            Arrays.asList(
                    "com.google.auto.value.extension.memoized.processor.MemoizedValidator",
                    "com.google.auto.value.processor.AutoAnnotationProcessor",
                    "com.google.auto.value.processor.AutoOneOfProcessor",
                    "com.google.auto.value.processor.AutoValueBuilderProcessor",
                    "com.google.auto.value.processor.AutoValueProcessor",
                    org.checkerframework.checker.objectconstruction.ObjectConstructionChecker.class
                            .getName());

    public ObjectConstructionAutoValueTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.objectconstruction.ObjectConstructionChecker.class,
                "objectconstruction-autovalue",
                "-Anomsgtext",
                "-nowarn");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"objectconstruction-autovalue"};
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
                buildDefaultConfiguration(
                        testDir,
                        testFiles,
                        ANNOTATION_PROCS,
                        customizedOptions,
                        shouldEmitDebugInfo);
        TypecheckResult testResult = new TypecheckExecutor().runTest(config);
        TestUtilities.assertResultsAreValid(testResult);
    }
}
