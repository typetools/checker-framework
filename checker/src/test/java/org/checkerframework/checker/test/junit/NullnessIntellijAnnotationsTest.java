package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit test for the Nullness Checker with IntelliJ IDEA annotations files. */
public class NullnessIntellijAnnotationsTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a NullnessIntellijAnnotationsTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public NullnessIntellijAnnotationsTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.nullness.NullnessChecker.class,
        "nullness",
        "-AintellijAnnotations=tests/nullness-intellijannotations");
  }

  /**
   * Returns the test directories for this test suite.
   *
   * @return array of test directory names
   */
  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"nullness-intellijannotations"};
  }
}
