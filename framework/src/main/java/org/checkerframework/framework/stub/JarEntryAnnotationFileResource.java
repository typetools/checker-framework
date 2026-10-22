package org.checkerframework.framework.stub;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * {@link JarEntry}-based implementation of {@link AnnotationFileResource}. The archive is opened
 * only while the entry is being read, so this resource holds no open file handle.
 */
public class JarEntryAnnotationFileResource implements AnnotationFileResource {
  /** The JAR or ZIP archive. */
  private final File archive;

  /** The name of the entry in the archive. */
  private final String entryName;

  /**
   * Constructs a {@code AnnotationFileResource} for the specified entry in the specified JAR file.
   *
   * @param archive the JAR or ZIP archive
   * @param entryName the name of the entry in the archive
   */
  public JarEntryAnnotationFileResource(File archive, String entryName) {
    this.archive = archive;
    this.entryName = entryName;
  }

  @Override
  public String getDescription() {
    return archive.getPath() + "!" + entryName;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    try (JarFile file = new JarFile(archive)) {
      JarEntry entry = file.getJarEntry(entryName);
      if (entry == null) {
        throw new IOException("No entry " + entryName + " in " + archive);
      }
      try (InputStream in = file.getInputStream(entry)) {
        return new ByteArrayInputStream(in.readAllBytes());
      }
    }
  }
}
