package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Index Checker when running together with the InitializedFields Checker. */
public class IndexListIndexingTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create an IndexTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public IndexListIndexingTest(List<File> testFiles) {
    super(
        testFiles,
        Arrays.asList("org.checkerframework.checker.index.IndexChecker"),
        "index-listindexing",
        Collections.emptyList(),
        "-AlistIndexing");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"index-listindexing"};
  }
}
