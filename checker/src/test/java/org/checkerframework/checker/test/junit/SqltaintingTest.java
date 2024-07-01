package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.sqltainting.SqlTaintingChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class SqltaintingTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a TaintingTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public SqltaintingTest(List<File> testFiles) {
    super(testFiles, SqlTaintingChecker.class, "sqltainting");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"sqltainting", "all-systems"};
  }
}
