package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.unboxingweakens.UnboxingWeakensChecker;
import org.junit.runners.Parameterized.Parameters;

/** Tests the UnboxingWeakens Checker, whose unboxing conversion weakens a qualifier. */
public class UnboxingWeakensTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Creates a new UnboxingWeakensTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public UnboxingWeakensTest(List<File> testFiles) {
    super(testFiles, UnboxingWeakensChecker.class, "unboxing-weakens");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"unboxing-weakens"};
  }
}
