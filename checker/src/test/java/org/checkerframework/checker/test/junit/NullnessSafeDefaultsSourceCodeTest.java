package org.checkerframework.checker.test.junit;

import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.test.TestConfiguration;
import org.checkerframework.framework.test.TestConfigurationBuilder;
import org.checkerframework.framework.test.TestUtilities;
import org.checkerframework.framework.test.TypecheckExecutor;
import org.checkerframework.framework.test.TypecheckResult;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
                NullnessChecker.class,
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
                        NullnessChecker.class,
                        customizedOptions1,
                        shouldEmitDebugInfo);
        TypecheckResult testResult1 = new TypecheckExecutor().runTest(config1);
        TestUtilities.assertTestDidNotFail(testResult1);

        List<String> customizedOptions2 =
                customizeOptions(Collections.unmodifiableList(checkerOptions));
        TestConfiguration config2 =
                TestConfigurationBuilder.buildDefaultConfiguration(
                        testDir,
                        testFiles,
                        Collections.singleton(NullnessChecker.class.getName()),
                        customizedOptions2,
                        shouldEmitDebugInfo);
        TypecheckResult testResult2 = new TypecheckExecutor().runTest(config2);
        TestUtilities.assertTestDidNotFail(testResult2);
    }
}
