package org.checkerframework.checker.test.junit;

// Test case for issue 691.
// https://github.com/typetools/checker-framework/issues/691
// This exists to just run the FormatterLubGlbChecker.

import java.io.File;
import java.util.List;
import org.checkerframework.checker.testchecker.lubglb.FormatterLubGlbChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class FormatterLubGlbCheckerTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a FormatterLubGlbCheckerTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public FormatterLubGlbCheckerTest(List<File> testFiles) {
    super(testFiles, FormatterLubGlbChecker.class, "", "-Anomsgtext", "-AcheckPurityAnnotations");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"formatter-lubglb"};
  }
}
