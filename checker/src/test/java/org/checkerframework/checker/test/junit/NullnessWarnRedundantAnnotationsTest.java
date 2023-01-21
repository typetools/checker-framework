package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Nullness checker when AwarnRedundantAnnotations is used. */
public class NullnessWarnRedundantAnnotationsTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a NullnessWarnRedundantAnnotationsTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public NullnessWarnRedundantAnnotationsTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.nullness.NullnessChecker.class,
        "nullness",
        "-Anomsgtext", // remove when #5556 is merged
        "-AwarnRedundantAnnotations");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"nullness-warnredundantannotations"};
  }
}
