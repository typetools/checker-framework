package org.checkerframework.framework.test;

import java.io.File;
import java.util.List;
import org.junit.runner.Runner;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;

/**
 * Encapsulates the directory root to search within for test files to parameterise the test with.
 */
abstract class RootedSuite extends Suite {

  /**
   * Called by this class and subclasses once the runners making up the suite have been determined.
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
    TestRootDirectory annotation = getTestClass().getAnnotation(TestRootDirectory.class);
    if (annotation != null) {
      return new File(annotation.value());
    }
    return new File("tests");
  }
}
