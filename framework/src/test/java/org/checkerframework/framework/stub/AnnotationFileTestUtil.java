package org.checkerframework.framework.stub;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.checkerframework.framework.stub.AnnotationFileUtil.AnnotationFileType;

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

  /**
   * Creates a jar archive that contains one entry with the given name and contents.
   *
   * @param jar an archive to create
   * @param entryName the name of the archive's entry
   * @param contents the contents of the archive's entry
   * @return {@code jar}
   * @throws IOException if the archive cannot be written
   */
  static File writeJar(File jar, String entryName, String contents) throws IOException {
    return writeJar(jar, Map.of(entryName, contents));
  }

  /**
   * Creates a jar archive that contains the given entries, in the iteration order of {@code
   * entries}.
   *
   * @param jar an archive to create
   * @param entries a map from the name of each entry of the archive to its contents
   * @return {@code jar}
   * @throws IOException if the archive cannot be written
   */
  static File writeJar(File jar, Map<String, String> entries) throws IOException {
    try (OutputStream os = Files.newOutputStream(jar.toPath());
        JarOutputStream jos = new JarOutputStream(os)) {
      for (Map.Entry<String, String> entry : entries.entrySet()) {
        jos.putNextEntry(new JarEntry(entry.getKey()));
        jos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
        jos.closeEntry();
      }
    }
    return jar;
  }

  /**
   * Returns the annotation files that {@link AnnotationFileUtil#forEachAnnotationFile} passes to
   * its action.
   *
   * @param location an annotation file, a jar or zip archive, or a directory
   * @param fileType the type of annotation files to return
   * @return the annotation files of type {@code fileType} at {@code location}, in the order that
   *     {@code forEachAnnotationFile} passes them
   */
  static List<AnnotationFileResource> annotationFiles(File location, AnnotationFileType fileType) {
    List<AnnotationFileResource> resources = new ArrayList<>();
    AnnotationFileUtil.forEachAnnotationFile(location, fileType, resources::add, skipped -> {});
    return resources;
  }
}
