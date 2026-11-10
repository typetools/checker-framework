package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.javacutil.BugInCF;
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
        "-Astubs=" + stubFilesInDirectoryOf(testFiles)
        // Cannot use this because of JUnit 5 stub file
        // "-AstubWarnIfNotFound",
        );
  }

  /**
   * Returns the stub files in the directory that contains {@code testFiles}, but not in its
   * subdirectories, separated by the path separator.
   *
   * @param testFiles the files in one test directory
   * @return the stub files in the directory that contains {@code testFiles}
   */
  private static String stubFilesInDirectoryOf(List<File> testFiles) {
    // Each directory is type-checked separately, so a stub file for a class in another directory
    // would cause a "Type not found" warning.
    File[] stubFiles =
        testFiles.get(0).getParentFile().listFiles((dir, name) -> name.endsWith(".astub"));
    if (stubFiles == null) {
      throw new BugInCF("Cannot list the files in %s", testFiles.get(0).getParentFile());
    }
    return Arrays.stream(stubFiles)
        .map(File::getPath)
        .sorted()
        .collect(Collectors.joining(File.pathSeparator));
  }

  @Parameterized.Parameters
  public static String[] getTestDirs() {
    return new String[] {"stubparser-records"};
  }
}
