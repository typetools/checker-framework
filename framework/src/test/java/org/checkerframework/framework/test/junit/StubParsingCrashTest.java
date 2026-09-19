package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.stubparsing.StubParsingCrashChecker;
import org.junit.runners.Parameterized.Parameters;

/** Tests that a failure while parsing stub files does not disable all later stub file lookups. */
public class StubParsingCrashTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public StubParsingCrashTest(List<File> testFiles) {
    super(testFiles, StubParsingCrashChecker.class, "simple");
  }

  /**
   * Returns the test directories.
   *
   * @return the test directories
   */
  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"simple"};
  }
}
