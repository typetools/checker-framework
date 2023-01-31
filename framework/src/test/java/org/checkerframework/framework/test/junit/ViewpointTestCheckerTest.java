package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

public class ViewpointTestCheckerTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public ViewpointTestCheckerTest(List<File> testFiles) {
    super(testFiles, viewpointtest.ViewpointTestChecker.class, "viewpointtest");
  }

  @Parameterized.Parameters
  public static String[] getTestDirs() {
    return new String[] {"viewpointtest"};
  }
}
