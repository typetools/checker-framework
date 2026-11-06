package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Nullness Checker, when the Path Shape Checker is disabled. */
public class NullnessNoPathShapeTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a NullnessNoPathShapeTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public NullnessNoPathShapeTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.nullness.NullnessChecker.class,
        "nullness",
        "-AnoPathShape");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"nullness-nopathshape"};
  }
}
