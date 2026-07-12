package org.checkerframework.checker.test.junit.ainferrunners;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class OptionalSideEffectsTest extends CheckerFrameworkPerDirectoryTest {

  public OptionalSideEffectsTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.optional.OptionalChecker.class,
        "optional",
        "-AcheckPurityAnnotations");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"optional-side-effects"};
  }
}
