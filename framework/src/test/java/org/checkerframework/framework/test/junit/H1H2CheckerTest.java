package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.h1h2checker.H1H2Checker;
import org.junit.runners.Parameterized.Parameters;

/** */
public class H1H2CheckerTest extends CheckerFrameworkPerDirectoryTest {

  /** @param testFiles the files containing test code, which will be type-checked */
  public H1H2CheckerTest(List<File> testFiles) {
    super(
        testFiles,
        H1H2Checker.class,
        "h1h2checker",
        "-Anomsgtext",
        "-Astubs=tests/h1h2checker/h1h2checker.astub");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"h1h2checker"};
  }
}
