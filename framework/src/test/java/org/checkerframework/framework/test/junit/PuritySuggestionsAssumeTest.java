package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.util.FlowTestChecker;
import org.junit.runners.Parameterized.Parameters;

/** Tests that {@code -AsuggestPureMethods} ignores the {@code -Aassume*} command-line arguments. */
public class PuritySuggestionsAssumeTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public PuritySuggestionsAssumeTest(List<File> testFiles) {
    super(
        testFiles,
        FlowTestChecker.class,
        "flow",
        "-AsuggestPureMethods",
        "-AcheckPurityAnnotations",
        "-AassumeSideEffectFree",
        "-AassumeDeterministic",
        "-AassumePureGetters");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"purity-suggestions-assume"};
  }
}
