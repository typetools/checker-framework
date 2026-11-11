package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.promotion.PromotionChecker;
import org.junit.runners.Parameterized.Parameters;

/** Tests the Promotion Checker. */
public class PromotionTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public PromotionTest(List<File> testFiles) {
    super(testFiles, PromotionChecker.class, "promotion");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"promotion"};
  }
}
