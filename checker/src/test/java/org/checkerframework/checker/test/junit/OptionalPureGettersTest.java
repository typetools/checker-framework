package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.optional.OptionalChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Optional Checker, which has the {@code @Present} annotation. */
public class OptionalPureGettersTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create an OptionalPureGettersTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public OptionalPureGettersTest(List<File> testFiles) {
    super(testFiles, OptionalChecker.class, "optional", "-AassumePureGetters");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"optional-pure-getters"};
  }
}
