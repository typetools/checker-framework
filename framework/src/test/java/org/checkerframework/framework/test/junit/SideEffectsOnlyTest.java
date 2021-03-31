package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class SideEffectsOnlyTest extends CheckerFrameworkPerDirectoryTest {

  /** @param testFiles the files containing test code, which will be type-checked */
  public SideEffectsOnlyTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.framework.testchecker.sideeffectsonly.SideEffectsOnlyToyChecker.class,
        "sideeffectsonly",
        "-Anomsgtext");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"sideeffectsonly"};
  }
}
