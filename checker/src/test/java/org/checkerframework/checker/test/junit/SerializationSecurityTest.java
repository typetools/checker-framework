package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

/** Test for Serialization Security Checker */
public class SerializationSecurityTest extends CheckerFrameworkPerDirectoryTest {

  /** Very nice documentation */
  public SerializationSecurityTest(List<File> testFiles) {
    super(
        testFiles, org.checkerframework.checker.ser03.SerializationSecurityChecker.class, "ser03");
  }

  /** Very nice documentation */
  @Parameterized.Parameters
  public static String[] getTestDirs() {
    return new String[] {"ser03"};
  }
}
