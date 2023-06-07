package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit test for the Nullness Checker with checking of enclosing expressions of inner class
 * instantiations enabled.
 */
public class NullnessEnclosingExprTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a NullnessEnclosingExprTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public NullnessEnclosingExprTest(List<File> testFiles) {
    super(testFiles, NullnessChecker.class, "nullness", "-AcheckEnclosingExpr");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"nullness-enclosingexpr", "all-systems"};
  }
}
