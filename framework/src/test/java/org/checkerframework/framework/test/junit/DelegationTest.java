package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class DelegationTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public DelegationTest(List<File> testFiles) {
    super(testFiles, org.checkerframework.common.delegation.DelegationChecker.class, "delegation");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"delegation"};
  }
}
