package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

public class ResourceLeakNpeNotIgnoredTest extends CheckerFrameworkPerDirectoryTest {
  public ResourceLeakNpeNotIgnoredTest(List<File> testFiles) {
    super(
        testFiles,
        ResourceLeakChecker.class,
        "resourceleak-npenotignored",
        "-AresourceLeakIgnoredExceptions=java.lang.Error",
        "-AwarnUnneededSuppressions",
        "-encoding",
        "UTF-8");
  }

  @Parameterized.Parameters
  public static String[] getTestDirs() {
    return new String[] {"resourceleak-npenotignored"};
  }
}
