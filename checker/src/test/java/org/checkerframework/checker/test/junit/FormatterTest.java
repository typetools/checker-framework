package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class FormatterTest extends CheckerFrameworkPerDirectoryTest {
  /**
   * Create a FormatterTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public FormatterTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.formatter.FormatterChecker.class,
        "formatter",
        "-Anomsgtext");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"formatter", "all-systems"};
  }
}
