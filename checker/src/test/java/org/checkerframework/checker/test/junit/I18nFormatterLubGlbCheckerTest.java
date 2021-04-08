package org.checkerframework.checker.test.junit;

// Test case for issue 723.
// https://github.com/typetools/checker-framework/issues/723
// This exists to just run the I18nFormatterLubGlbChecker.

import java.io.File;
import java.util.List;
import org.checkerframework.checker.testchecker.lubglb.I18nFormatterLubGlbChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class I18nFormatterLubGlbCheckerTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create an I18nFormatterLubGlbCheckerTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public I18nFormatterLubGlbCheckerTest(List<File> testFiles) {
    super(
        testFiles, I18nFormatterLubGlbChecker.class, "", "-Anomsgtext", "-AcheckPurityAnnotations");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"i18n-formatter-lubglb"};
  }
}
