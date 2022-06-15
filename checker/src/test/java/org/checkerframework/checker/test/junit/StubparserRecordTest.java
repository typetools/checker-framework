package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

/** Tests for stub parsing with records. */
public class StubparserRecordTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a StubparserRecordTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public StubparserRecordTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.nullness.NullnessChecker.class,
        "stubparser-records",
        "-Anomsgtext",
        "-Astubs=tests/stubparser-records",
        "-AstubWarnIfNotFound");
  }

  @Parameterized.Parameters
  public static String[] getTestDirs() {
    // Check for JDK 16+ without using a library:
    // There is no decimal point in the JDK 17 version number.
    if (System.getProperty("java.version").matches("^(1[6-9]|[2-9][0-9])")) {
      return new String[] {"stubparser-records"};
    } else {
      return new String[] {};
    }
  }
}
