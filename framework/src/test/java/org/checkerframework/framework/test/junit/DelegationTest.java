package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Delegation Checker. */
public class DelegationTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Creates a new DelegationTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public DelegationTest(List<File> testFiles) {
    super(testFiles, org.checkerframework.common.delegation.DelegationChecker.class, "delegation");
  }

  /**
   * Returns the directories that contain the test files.
   *
   * @return the directories that contain the test files
   */
  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"delegation"};
  }
}
