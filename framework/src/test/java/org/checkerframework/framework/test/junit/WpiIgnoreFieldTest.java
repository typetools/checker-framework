package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.wpiignorefield.WpiIgnoreFieldChecker;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests that {@code WholeProgramInferenceImplementation.updateFieldFromType} defers to {@code
 * ignoreFieldInWPI} when deciding which fields to skip.
 */
public class WpiIgnoreFieldTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public WpiIgnoreFieldTest(List<File> testFiles) {
    super(testFiles, WpiIgnoreFieldChecker.class, "wpiignorefield");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"wpiignorefield"};
  }
}
