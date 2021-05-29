package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class I18nFormatterTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create an I18nFormatterTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public I18nFormatterTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.i18nformatter.I18nFormatterChecker.class,
        "i18n-formatter",
        "-Anomsgtext");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"i18n-formatter", "all-systems"};
  }
}
