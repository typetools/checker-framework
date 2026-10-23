package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.util.FlowTestChecker;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests that no purity diagnostic is issued without {@code -AcheckPurityAnnotations}. The test
 * files contain purity violations and expect no diagnostic, so this test fails if a purity check
 * runs unconditionally.
 */
public class PurityNotCheckedTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public PurityNotCheckedTest(List<File> testFiles) {
    super(testFiles, FlowTestChecker.class, "purity-not-checked");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"purity-not-checked"};
  }
}
