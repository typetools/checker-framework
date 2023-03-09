package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness Checker -- testing type-checking of code that uses Javadoc classes.
 */
public class NullnessJavadocTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public NullnessJavadocTest(List<File> testFiles) {
    super(testFiles, org.checkerframework.checker.nullness.NullnessChecker.class, "nullness");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"nullness-javadoc"};
  }
}
