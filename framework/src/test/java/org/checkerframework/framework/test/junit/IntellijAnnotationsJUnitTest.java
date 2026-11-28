package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.h1h2checker.H1H2Checker;
import org.junit.runners.Parameterized.Parameters;

/** JUnit test for IntelliJ IDEA annotations file support. */
public class IntellijAnnotationsJUnitTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public IntellijAnnotationsJUnitTest(List<File> testFiles) {
    super(
        testFiles,
        H1H2Checker.class,
        "intellij-annotations",
        "-AintellijAnnotations=tests/intellijannotations");
  }

  /**
   * Returns the test directories for this test suite.
   *
   * @return array of test directory names
   */
  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"intellijannotations"};
  }
}
