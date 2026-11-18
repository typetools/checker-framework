package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Nullness Checker, with the stub file {@code sound-file-topath.astub}. */
public class NullnessSoundFileToPathTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a NullnessSoundFileToPathTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public NullnessSoundFileToPathTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.nullness.NullnessChecker.class,
        "nullness",
        "-Astubs=sound-file-topath.astub");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"nullness-sound-file-topath"};
  }
}
