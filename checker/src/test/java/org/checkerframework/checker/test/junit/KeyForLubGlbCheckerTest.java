package org.checkerframework.checker.test.junit;

// This exists to just run the KeyForLubGlbChecker.

import java.io.File;
import java.util.List;
import org.checkerframework.checker.testchecker.lubglb.KeyForLubGlbChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class KeyForLubGlbCheckerTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a KeyForLubGlbCheckerTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public KeyForLubGlbCheckerTest(List<File> testFiles) {
    super(testFiles, KeyForLubGlbChecker.class, "");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"keyfor-lubglb"};
  }
}
