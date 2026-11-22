package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.h1h2checker.PartialBoundsH1H2Checker;
import org.junit.runners.Parameterized.Parameters;

/** Tests a checker whose default qualifier upper bounds omit a type hierarchy. */
public class PartialBoundsH1H2CheckerTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Creates a new PartialBoundsH1H2CheckerTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public PartialBoundsH1H2CheckerTest(List<File> testFiles) {
    super(testFiles, PartialBoundsH1H2Checker.class, "h1h2checker-partial-bounds");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"h1h2checker-partial-bounds"};
  }
}
