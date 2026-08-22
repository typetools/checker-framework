package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.tainting.TaintingChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests that {@code @SideEffectsOnly} annotations are not checked against method bodies without
 * {@code -AcheckPurityAnnotations}, even when {@code -AsuggestPureMethods} is supplied. Issuing a
 * purity suggestion does not require {@code -AcheckPurityAnnotations}, so the two options must not
 * be conflated.
 */
public class SideEffectsOnlyNoCheckTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a SideEffectsOnlyNoCheckTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public SideEffectsOnlyNoCheckTest(List<File> testFiles) {
    super(testFiles, TaintingChecker.class, "sideeffectsonly-nocheck", "-AsuggestPureMethods");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"sideeffectsonly-nocheck"};
  }
}
