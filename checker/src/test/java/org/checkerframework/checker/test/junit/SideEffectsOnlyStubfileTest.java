package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.tainting.TaintingChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests {@code @SideEffectsOnly} annotations that are supplied by a stub file, so that they are
 * never checked at a declaration.
 */
public class SideEffectsOnlyStubfileTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a SideEffectsOnlyStubfileTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public SideEffectsOnlyStubfileTest(List<File> testFiles) {
    super(
        testFiles,
        TaintingChecker.class,
        "sideeffectsonly-stubfile",
        "-AcheckPurityAnnotations",
        "-Astubs=tests/sideeffectsonly-stubfile/seonly.astub");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"sideeffectsonly-stubfile"};
  }
}
