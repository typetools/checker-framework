package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

/** Test for Serialization Security Checker */
public class SerializationSecurityTest extends CheckerFrameworkPerDirectoryTest {

  /** Create test for the checker */
  public SerializationSecurityTest(List<File> testFiles) {
    super(
        testFiles, org.checkerframework.checker.serialization.SerializationSecurityChecker.class, "serialization");
  }

  @Parameterized.Parameters
  public static String[] getTestDirs() {
    return new String[] {"serialization"};
  }
}
