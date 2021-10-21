package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Nullness checker with records (JDK16+ only). */
public class NullnessRecordsTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a NullnessRecordsTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public NullnessRecordsTest(List<File> testFiles) {
    super(
        testFiles,
        NullnessChecker.class,
        "nullness-records",
        "-AcheckPurityAnnotations",
        "-Anomsgtext",
        "-Xlint:deprecation");
  }

  @Parameters
  public static String[] getTestDirs() {
    // Check for JDK 16+ without using a library:
    // There is no decimal point in the JDK 17 version number.
    if (System.getProperty("java.version").matches("^(1[6-9]|[2-9][0-9])"))
      return new String[] {"nullness-records"};
    else return new String[] {};
  }
}
