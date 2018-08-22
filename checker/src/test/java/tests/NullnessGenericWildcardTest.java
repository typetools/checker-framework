package tests;

import static org.checkerframework.framework.test.TestConfigurationBuilder.buildDefaultConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.checkerframework.framework.test.*;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Nullness checker for issue #511. */
public class NullnessGenericWildcardTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public NullnessGenericWildcardTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
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
                buildDefaultConfiguration(
                        "tests/nullness-genericwildcardlib",
                        new File("tests/nullness-genericwildcardlib", "GwiParent.java"),
                        checkerName,
                        customizedOptions1,
                        shouldEmitDebugInfo);
        TypecheckResult testResult1 = new TypecheckExecutor().runTest(config1);
        TestUtilities.assertResultsAreValid(testResult1);

        List<String> customizedOptions2 =
                customizeOptions(Collections.unmodifiableList(checkerOptions));
        TestConfiguration config2 =
                buildDefaultConfiguration(
                        testDir,
                        testFiles,
                        Collections.singleton(checkerName),
                        customizedOptions2,
                        shouldEmitDebugInfo);
        TypecheckResult testResult2 = new TypecheckExecutor().runTest(config2);
        TestUtilities.assertResultsAreValid(testResult2);
    }
}
