// TODO: When plume-util 1.6.0 is released, deprecate createTempFile in favor of
// FilesPlume.createTempFile().
package org.checkerframework.javacutil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;

/** File utilities. To be moved into plume-util. */
public class FileUtils {

  /** Do not instantiate. */
  private FileUtils() {
    throw new Error("do not instantiate");
  }

  /**
   * Creates a new empty file in the default temporary-file directory, using the given prefix and
   * suffix strings to generate its name. This is like {@link File#createTempFile}, but uses
   * sequential file names.
   *
   * @param prefix the prefix string to be used in generating the file's name; may be null
   * @param suffix the suffix string to be used in generating the file's name; may be null, in which
   *     case ".tmp" is used
   * @param attrs an optional list of file attributes to set atomically when creating the file
   * @return the path to the newly created file that did not exist before this method was invoked
   * @throws IllegalArgumentException if there is trouble creating the file
   */
  public static Path createTempFile(String prefix, String suffix, FileAttribute<?>... attrs)
      throws IOException {
    return createTempFile(Path.of(System.getProperty("java.io.tmpdir")), prefix, suffix, attrs);
  }

  /**
   * Creates a new empty file in the specified directory, using the given prefix and suffix strings
   * to generate its name. This is like {@link File#createTempFile}, but uses sequential file names.
   *
   * @param dir the path to directory in which to create the file
   * @param prefix the prefix string to be used in generating the file's name; may be null
   * @param suffix the suffix string to be used in generating the file's name; may be null, in which
   *     case ".tmp" is used
   * @param attrs an optional list of file attributes to set atomically when creating the file
   * @return the path to the newly created file that did not exist before this method was invoked
   * @throws IllegalArgumentException if there is trouble creating the file
   */
  public static Path createTempFile(
      Path dir, String prefix, String suffix, FileAttribute<?>... attrs) throws IOException {
    Path createdDir = Files.createDirectories(dir, attrs);
    for (int i = 1; i < Integer.MAX_VALUE; i++) {
      File candidate = new File(createdDir.toFile(), prefix + i + suffix);
      if (!candidate.exists()) {
        System.out.println("Created " + candidate);
        return candidate.toPath();
      }
    }
    throw new Error("every file exists");
  }
}
