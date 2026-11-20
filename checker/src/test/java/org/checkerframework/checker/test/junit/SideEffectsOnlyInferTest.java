package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.tainting.TaintingChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests that an overriding method is not checked against the overridden method's {@code
 * SideEffectsOnly} annotation under {@code -Ainfer} without {@code -AcheckPurityAnnotations}.
 */
public class SideEffectsOnlyInferTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a SideEffectsOnlyInferTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public SideEffectsOnlyInferTest(List<File> testFiles) {
    super(
        testFiles,
        TaintingChecker.class,
        "sideeffectsonly-infer",
        "-Ainfer=ajava",
        "-AinferOutputDirectory=build/sideeffectsonly-infer-output",
        // -Ainfer requires -Awarns.  A diagnostic is then a warning, so the test still fails if
        // the override is checked.
        "-Awarns");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"sideeffectsonly-infer"};
  }
}
