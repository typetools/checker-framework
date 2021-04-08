package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.flowexpression.FlowExpressionChecker;
import org.junit.runners.Parameterized.Parameters;

public class FlowExpressionCheckerTest extends CheckerFrameworkPerDirectoryTest {

  /** @param testFiles the files containing test code, which will be type-checked */
  public FlowExpressionCheckerTest(List<File> testFiles) {
    super(testFiles, FlowExpressionChecker.class, "flowexpression", "-Anomsgtext");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"flowexpression", "all-systems"};
  }
}
