package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests a precedence list for which applying every default in a single traversal of the type would
 * not give the same result as traversing the type once per default.
 */
public class SinglePassDefaultTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public SinglePassDefaultTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.framework.testchecker.singlepassdefault.SinglePassDefaultChecker.class,
        "singlepassdefault");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"singlepassdefault"};
  }
}
