package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.test.TestConfiguration;
import org.checkerframework.framework.test.TestConfigurationBuilder;
import org.checkerframework.framework.test.TestUtilities;
import org.checkerframework.framework.test.TypecheckExecutor;
import org.checkerframework.framework.test.TypecheckResult;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Nullness checker for issue #511. */
public class NullnessGenericWildcardTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessGenericWildcardTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessGenericWildcardTest(List<File> testFiles) {
        super(
                testFiles,
                NullnessChecker.class,
                "nullness",
                // This test reads bytecode .class files created by NullnessGenericWildcardLibTest
                "-cp",
                "dist/checker.jar:tests/build/testclasses/",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-genericwildcard"};
    }

    @Override
    public void run() {
        boolean shouldEmitDebugInfo = TestUtilities.getShouldEmitDebugInfo();
        List<String> customizedOptions1 = customizeOptions(Arrays.asList("-Anomsgtext"));
        TestConfiguration config1 =
                TestConfigurationBuilder.buildDefaultConfiguration(
                        "tests/nullness-genericwildcardlib",
                        new File("tests/nullness-genericwildcardlib", "GwiParent.java"),
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
