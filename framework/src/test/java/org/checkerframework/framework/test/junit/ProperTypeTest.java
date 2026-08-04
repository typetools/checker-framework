package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.typeinference8.ProperTypeChecker;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for invariants of the type argument inference implementation. */
public class ProperTypeTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public ProperTypeTest(List<File> testFiles) {
    super(testFiles, ProperTypeChecker.class, "typeinference8");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"typeinference8"};
  }
}
