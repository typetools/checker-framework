package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Tests for the Resource Leak Checker that require the Returns Receiver Checker to be enabled. */
public class ResourceLeakReturnsReceiverTest extends CheckerFrameworkPerDirectoryTest {
  public ResourceLeakReturnsReceiverTest(List<File> testFiles) {
    super(
        testFiles,
        ResourceLeakChecker.class,
        "resourceleak",
        "-AwarnUnneededSuppressions",
        "-AenableReturnsReceiverForRlc",
        "-encoding",
        "UTF-8");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"resourceleak-returns-receiver"};
  }
}
