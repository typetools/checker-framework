package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Interning Checker when -Acheckclass is used. */
public class InterningCheckClassTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Creates an InterningCheckClassTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public InterningCheckClassTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.interning.InterningChecker.class,
        "interning",
        "-Acheckclass=InterningCheckClass.A");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"interning-checkclass"};
  }
}
