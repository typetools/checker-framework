package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests a checker whose applier overrides {@code
 * QualifierDefaults.DefaultApplierElement.addAnnotation}, for which every default must be applied,
 * including the ones that {@code QualifierDefaults.minimizeDefaults} would remove.
 */
public class CustomApplierTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public CustomApplierTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.framework.testchecker.customapplier.CustomApplierChecker.class,
        "customapplier");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"customapplier"};
  }
}
