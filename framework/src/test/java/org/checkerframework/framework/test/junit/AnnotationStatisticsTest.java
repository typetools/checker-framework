package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.common.util.count.AnnotationStatistics;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for {@link AnnotationStatistics}, a checker that extends {@code SourceChecker}
 * directly, not via {@code BaseTypeChecker}.
 */
public class AnnotationStatisticsTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Creates a new AnnotationStatisticsTest.
   *
   * @param testFiles the files containing test code, which will be processed
   */
  public AnnotationStatisticsTest(List<File> testFiles) {
    super(testFiles, AnnotationStatistics.class, "annotationstatistics");
  }

  /**
   * Returns the directories that contain the test files.
   *
   * @return the directories that contain the test files
   */
  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"annotationstatistics"};
  }
}
