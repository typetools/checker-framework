package org.checkerframework.framework.stub;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Utility methods for the tests of annotation files. */
final class AnnotationFileTestUtil {

  /** Do not instantiate. */
  private AnnotationFileTestUtil() {
    throw new Error("Do not instantiate");
  }

  /**
   * Creates a file with the given contents, creating its parent directories if necessary.
   *
   * @param file the file to create
   * @param contents the contents of the file
   * @return {@code file}
   * @throws IOException if the file cannot be written
   */
  static File write(File file, String contents) throws IOException {
    File parent = file.getParentFile();
    if (parent != null) {
      Files.createDirectories(parent.toPath());
    }
    Files.write(file.toPath(), contents.getBytes(StandardCharsets.UTF_8));
    return file;
  }
}
