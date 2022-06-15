package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Tests for the Resource Leak Checker. */
public class ResourceLeakNoLightweightOwnershipTest extends CheckerFrameworkPerDirectoryTest {
  public ResourceLeakNoLightweightOwnershipTest(List<File> testFiles) {
    super(
        testFiles,
        ResourceLeakChecker.class,
        "resourceleak-nolightweightownership",
        "-Anomsgtext",
        "-AnoLightweightOwnership",
        "-nowarn",
        "-encoding",
        "UTF-8");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"resourceleak-nolightweightownership"};
  }
}
