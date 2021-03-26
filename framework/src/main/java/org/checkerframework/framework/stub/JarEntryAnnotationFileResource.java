package org.checkerframework.framework.stub;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** {@link JarEntry}-based implementation of {@link AnnotationFileResource}. */
public class JarEntryAnnotationFileResource implements AnnotationFileResource {
  /** The underlying JarFile. */
  private final JarFile file;
  /** The entry in the jar file. */
  private final JarEntry entry;

  /**
   * Constructs a {@code AnnotationFileResource} for the specified entry in the specified JAR file.
   *
   * @param file the JAR file
   * @param entry the JAR entry
   */
  public JarEntryAnnotationFileResource(JarFile file, JarEntry entry) {
    this.file = file;
    this.entry = entry;
  }

  @Override
  public String getDescription() {
    return file.getName() + "!" + entry.getName();
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return file.getInputStream(entry);
  }
}
