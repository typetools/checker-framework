package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class SignednessUncheckedDefaultsTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a SignednessUncheckedDefaultsTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public SignednessUncheckedDefaultsTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.signedness.SignednessChecker.class,
        "signedness",
        "-Anomsgtext",
        "-AuseConservativeDefaultsForUncheckedCode=-source,bytecode");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"signedness-unchecked-defaults"};
  }
}
