package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class I18nUncheckedDefaultsTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create an I18nUncheckedDefaultsTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public I18nUncheckedDefaultsTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.i18n.I18nChecker.class,
        "i18n",
        "-Anomsgtext",
        "-AuseConservativeDefaultsForUncheckedCode=-source,bytecode");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"i18n-unchecked-defaults"};
  }
}
