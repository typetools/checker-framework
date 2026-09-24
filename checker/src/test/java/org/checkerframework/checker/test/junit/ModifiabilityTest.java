package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.modifiability.ModifiabilityChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class ModifiabilityTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a ModifiabilityTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public ModifiabilityTest(List<File> testFiles) {
    super(testFiles, ModifiabilityChecker.class, "modifiability", "-Anomsgtext");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"modifiability", "all-systems"};
  }
}
