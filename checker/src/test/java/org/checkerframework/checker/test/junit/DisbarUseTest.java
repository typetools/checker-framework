package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.testchecker.disbaruse.DisbarUseChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class DisbarUseTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a DisbarUseTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public DisbarUseTest(List<File> testFiles) {
    super(
        testFiles,
        DisbarUseChecker.class,
        "disbaruse-records",
        "-Astubs=tests/disbaruse-records",
        "-AstubWarnIfNotFound");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"disbaruse-records"};
  }
}
