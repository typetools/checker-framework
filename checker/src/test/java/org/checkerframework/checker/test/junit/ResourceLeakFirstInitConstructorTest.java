package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for validating safe suppression of resource leak warnings when a private field is
 * initialized for the first time inside a constructor.
 *
 * <p>These tests check that the checker allows first-time constructor-based assignments (when safe)
 * and continues to report reassignments or leaks in all other cases (e.g., after method calls,
 * initializer blocks, etc.).
 */
public class ResourceLeakFirstInitConstructorTest extends CheckerFrameworkPerDirectoryTest {
  public ResourceLeakFirstInitConstructorTest(List<File> testFiles) {
    super(
        testFiles,
        ResourceLeakChecker.class,
        "resourceleak-firstinitconstructor",
        "-AwarnUnneededSuppressions",
        "-encoding",
        "UTF-8");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"resourceleak-firstinitconstructor"};
  }
}
