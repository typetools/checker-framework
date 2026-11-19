package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.util.FlowTestChecker;
import org.junit.runners.Parameterized.Parameters;

/** Tests the diagnostics that {@code -AwarnRedundantAnnotations} enables. */
public class FlowWarnRedundantAnnotationsTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public FlowWarnRedundantAnnotationsTest(List<File> testFiles) {
    super(
        testFiles,
        FlowTestChecker.class,
        "flow-warnredundantannotations",
        "-AwarnRedundantAnnotations");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"flow-warnredundantannotations"};
  }
}
