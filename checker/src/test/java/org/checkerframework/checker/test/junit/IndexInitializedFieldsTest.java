package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Index Checker when running together with the InitializedFields Checker. */
public class IndexInitializedFieldsTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create an IndexTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public IndexInitializedFieldsTest(List<File> testFiles) {
    super(
        testFiles,
        List.of(
            "org.checkerframework.checker.index.IndexChecker",
            "org.checkerframework.common.initializedfields.InitializedFieldsChecker"),
        "index-initializedfields",
        Collections.emptyList(),
        "-Anomsgtext",
        "-Aajava=tests/index-initializedfields/input-annotation-files/");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"index-initializedfields"};
  }
}
