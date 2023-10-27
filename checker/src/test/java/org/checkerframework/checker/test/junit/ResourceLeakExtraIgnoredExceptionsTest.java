package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

public class ResourceLeakExtraIgnoredExceptionsTest extends CheckerFrameworkPerDirectoryTest {
  public ResourceLeakExtraIgnoredExceptionsTest(List<File> testFiles) {
    super(
        testFiles,
        ResourceLeakChecker.class,
        "resourceleak-extraignoredexceptions",
        "-AresourceLeakIgnoredExceptions=default,java.lang.IllegalStateException",
        "-AwarnUnneededSuppressions",
        "-encoding",
        "UTF-8");
  }

  @Parameterized.Parameters
  public static String[] getTestDirs() {
    return new String[] {"resourceleak-extraignoredexceptions"};
  }
}
