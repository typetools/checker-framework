package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Tests that QualifierDefaults.addElementDefault composes with the other defaults. */
public class ElementDefaultTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public ElementDefaultTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.framework.testchecker.elementdefault.ElementDefaultChecker.class,
        "elementdefault");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"elementdefault"};
  }
}
