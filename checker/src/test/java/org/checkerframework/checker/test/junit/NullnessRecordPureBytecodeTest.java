package org.checkerframework.checker.test.junit;

import java.io.File;
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

/** JUnit tests that compiler-generated record accessors are pure when read from bytecode. */
public class NullnessRecordPureBytecodeTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a NullnessRecordPureBytecodeTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public NullnessRecordPureBytecodeTest(List<File> testFiles) {
    super(
        testFiles,
        NullnessChecker.class,
        "nullness",
        // This test reads bytecode .class files created by the first phase of run()
        "-cp",
        "dist/checker.jar:tests/build/testclasses/");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"nullness-recordpure"};
  }

  @Override
  public void run() {
    boolean shouldEmitDebugInfo = TestUtilities.getShouldEmitDebugInfo();
    List<String> customizedOptions1 = customizeOptions(Collections.emptyList());
    TestConfiguration config1 =
        TestConfigurationBuilder.buildDefaultConfiguration(
            "tests/nullness-recordpurelib",
            new File("tests/nullness-recordpurelib", "RecordPureLib.java"),
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
