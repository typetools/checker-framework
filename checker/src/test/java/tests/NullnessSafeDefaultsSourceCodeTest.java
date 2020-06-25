package tests;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.checkerframework.framework.test.*;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Nullness checker when using safe defaults for unannotated source code. */
public class NullnessSafeDefaultsSourceCodeTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessSafeDefaultsSourceCodeTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessSafeDefaultsSourceCodeTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AuseConservativeDefaultsForUncheckedCode=source",
                "-cp",
                "dist/checker.jar:tests/build/testclasses/",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-safedefaultssourcecode"};
    }

    @Override
    public void run() {
        boolean shouldEmitDebugInfo = TestUtilities.getShouldEmitDebugInfo();
        List<String> customizedOptions1 =
                customizeOptions(
                        Arrays.asList(
                                "-AuseConservativeDefaultsForUncheckedCode=source,bytecode",
                                "-Anomsgtext"));
        TestConfiguration config1 =
                TestConfigurationBuilder.buildDefaultConfiguration(
                        "tests/nullness-safedefaultssourcecodelib",
                        new File("tests/nullness-safedefaultssourcecodelib", "Lib.java"),
                        checkerName,
                        customizedOptions1,
                        shouldEmitDebugInfo);
        TypecheckResult testResult1 = new TypecheckExecutor().runTest(config1);
        TestUtilities.assertResultsAreValid(testResult1);

        List<String> customizedOptions2 =
                customizeOptions(Collections.unmodifiableList(checkerOptions));
        TestConfiguration config2 =
                TestConfigurationBuilder.buildDefaultConfiguration(
                        testDir,
                        testFiles,
                        Collections.singleton(checkerName),
                        customizedOptions2,
                        shouldEmitDebugInfo);
        TypecheckResult testResult2 = new TypecheckExecutor().runTest(config2);
        TestUtilities.assertResultsAreValid(testResult2);
    }
}
