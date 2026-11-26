package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.common.util.count.JavaCodeStatistics;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for {@link JavaCodeStatistics}, a checker that extends {@code SourceChecker}
 * directly, not via {@code BaseTypeChecker}.
 */
public class JavaCodeStatisticsTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Creates a new JavaCodeStatisticsTest.
   *
   * @param testFiles the files containing test code, which will be processed
   */
  public JavaCodeStatisticsTest(List<File> testFiles) {
    super(testFiles, JavaCodeStatistics.class, "javacodestatistics");
  }

  /**
   * Returns the directories that contain the test files.
   *
   * @return the directories that contain the test files
   */
  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"javacodestatistics"};
  }
}
