package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.confidential.ConfidentialChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class ConfidentialTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a ConfidentialTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public ConfidentialTest(List<File> testFiles) {
    super(testFiles, ConfidentialChecker.class, "confidential");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"confidential", "all-systems"};
  }
}
