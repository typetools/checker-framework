package org.checkerframework.framework.test;

import java.io.File;
import java.util.List;
import java.util.Optional;
import org.junit.runner.Runner;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;

/**
 * Encapsulates the directory root to search within for test files to parameterise the test with.
 */
abstract class RootedSuite extends Suite {

  /**
   * Called by this class and subclasses once the runners making up the suite have been determined
   *
   * @param klass root of the suite
   * @param runners for each class in the suite, a {@link Runner}
   * @throws InitializationError malformed test suite
   */
  public RootedSuite(Class<?> klass, List<Runner> runners) throws InitializationError {
    super(klass, runners);
  }

  /**
   * Resolves the directory specified by {@link TestRootDirectory} or defaults to {@code
   * currentDir/tests}.
   *
   * @return the resolved directory
   */
  protected final File resolveTestDirectory() {
    return Optional.ofNullable(getTestClass().getAnnotation(TestRootDirectory.class))
        .map(TestRootDirectory::value)
        .map(File::new)
        .orElseGet(() -> new File("tests"));
  }
}
