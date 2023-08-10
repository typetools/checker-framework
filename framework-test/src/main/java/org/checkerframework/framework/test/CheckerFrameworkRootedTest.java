package org.checkerframework.framework.test;

import java.io.File;
import java.util.Optional;

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
    return Optional.ofNullable(getClass().getAnnotation(TestRootDirectory.class))
        .map(TestRootDirectory::value)
        .map(File::new)
        .orElseGet(() -> new File("tests"));
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
