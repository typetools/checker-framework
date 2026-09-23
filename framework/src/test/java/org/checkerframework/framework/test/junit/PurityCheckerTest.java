package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.util.PurityChecker;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests that the Purity Checker issues purity diagnostics without {@code -AcheckPurityAnnotations}.
 */
public class PurityCheckerTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public PurityCheckerTest(List<File> testFiles) {
    super(testFiles, PurityChecker.class, "purity-checker");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"purity-checker"};
  }
}
