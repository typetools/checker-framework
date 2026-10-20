package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.optional.OptionalChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests that the Optional Checker retains type refinements across a call to a method that is
 * annotated with {@code @SideEffectsOnly}.
 */
public class OptionalSideEffectsTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create an OptionalSideEffectsTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public OptionalSideEffectsTest(List<File> testFiles) {
    super(testFiles, OptionalChecker.class, "optional-side-effects", "-AcheckPurityAnnotations");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"optional-side-effects"};
  }
}
