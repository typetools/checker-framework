package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness Checker -- testing {@code -Alint=permitClearProperty} command-line
 * argument.
 */
public class NullnessPermitClearPropertyTest extends CheckerFrameworkPerDirectoryTest {

  /** @param testFiles the files containing test code, which will be type-checked */
  public NullnessPermitClearPropertyTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.nullness.NullnessChecker.class,
        "nullness",
        "-Anomsgtext",
        "-Alint=permitClearProperty");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"nullness-permitClearProperty"};
  }
}
