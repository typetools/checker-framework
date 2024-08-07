package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.sqlquotes.SqlQuotesChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class SqlQuotesTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a SqlQuotesTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public SqlQuotesTest(List<File> testFiles) {
    super(testFiles, SqlQuotesChecker.class, "sqlquotes");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"sqlquotes", "all-systems"};
  }
}
