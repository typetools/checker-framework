package org.checkerframework.framework.test;

import java.io.File;

/** Encapsulates the directory root to search within for test files to compile. */
abstract class CheckerFrameworkRootedTest {

  /** Constructs a test that will assert that can resolve its tests root directory. */
  public CheckerFrameworkRootedTest() {}

  /**
   * Resolves the test root directory from the optional {@link TestRootDirectory} annotation or
   * falls back to the default of {@code currentDir/tests}.
   *
   * @return the resolved directory
   */
  protected File resolveTestDirectory() {
    TestRootDirectory annotation = getClass().getAnnotation(TestRootDirectory.class);
    if (annotation != null) {
      return new File(annotation.value());
    }
    return new File("test");
  }

  /**
   * Check that the {@link TypecheckResult} did not fail.
   *
   * @param typecheckResult result to check
   */
  public void checkResult(TypecheckResult typecheckResult) {
    TestUtilities.assertTestDidNotFail(typecheckResult);
  }
}
