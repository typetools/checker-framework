package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Nullness Checker. */
public class NullnessEnableRegexCheckerTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a NullnessEnableRegexCheckerTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public NullnessEnableRegexCheckerTest(List<File> testFiles) {
    super(testFiles, NullnessChecker.class, "nullness", "-AenableRegexChecker", "-Anomsgtext");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"nullness-enableregexchecker"};
  }
}
