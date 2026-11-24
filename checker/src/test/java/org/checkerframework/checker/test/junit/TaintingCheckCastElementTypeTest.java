package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.tainting.TaintingChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Tainting Checker when checkCastElementType is used. */
public class TaintingCheckCastElementTypeTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Creates a TaintingCheckCastElementTypeTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public TaintingCheckCastElementTypeTest(List<File> testFiles) {
    super(testFiles, TaintingChecker.class, "tainting", "-AcheckCastElementType");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"tainting-checkcastelementtype"};
  }
}
