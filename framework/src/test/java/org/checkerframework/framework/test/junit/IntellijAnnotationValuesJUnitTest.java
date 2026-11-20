package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.testaccumulation.TestAccumulationChecker;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit test for annotation element values in an IntelliJ IDEA annotations file. It uses an
 * accumulation checker because that checker's qualifiers have elements.
 */
public class IntellijAnnotationValuesJUnitTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public IntellijAnnotationValuesJUnitTest(List<File> testFiles) {
    super(
        testFiles,
        TestAccumulationChecker.class,
        "intellij-annotation-values",
        "-AintellijAnnotations=tests/intellijannotationvalues");
  }

  /**
   * Returns the test directories for this test suite.
   *
   * @return array of test directory names
   */
  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"intellijannotationvalues"};
  }
}
