package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.lubglb.LubGlbChecker;
import org.junit.runners.Parameterized.Parameters;

/** */
public class LubGlbTest extends CheckerFrameworkPerDirectoryTest {

  /** @param testFiles the files containing test code, which will be type-checked */
  public LubGlbTest(List<File> testFiles) {
    super(testFiles, LubGlbChecker.class, "lubglb", "-Anomsgtext");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"lubglb"};
  }
}
