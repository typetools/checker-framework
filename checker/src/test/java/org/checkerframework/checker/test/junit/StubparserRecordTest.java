package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

/** Tests for stub parsing with records. */
public class StubparserRecordTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a StubparserRecordTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public StubparserRecordTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.nullness.NullnessChecker.class,
        "stubparser-records",
        "-Astubs=tests/stubparser-records",
        // NestedRecord.astub deliberately names a record component that does not exist.  A
        // warning about an annotation file is issued at line 0 of no file, so it cannot be
        // written as an expected diagnostic.
        "-AstubNoWarnIfNotFound"
        // Cannot use this because of JUnit 5 stub file
        // "-AstubWarnIfNotFound",
        );
  }

  @Parameterized.Parameters
  public static String[] getTestDirs() {
    return new String[] {"stubparser-records"};
  }
}
