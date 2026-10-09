package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.util.EvenOddChecker;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for reading annotations from an ajava file. */
public class AjavaTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Creates an AjavaTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public AjavaTest(List<File> testFiles) {
    super(
        testFiles,
        EvenOddChecker.class,
        "ajava",
        "-Anomsgtext",
        "-Aajava=tests/ajava/input-annotation-files/");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"ajava"};
  }
}
