package org.checkerframework.framework.stub;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

/**
 * Implementation of {@link AnnotationFileResource} for an entry in a JAR or ZIP archive. The
 * resource holds the entry's contents, so it holds no open file handle.
 */
public class JarEntryAnnotationFileResource implements AnnotationFileResource {
  /** The JAR or ZIP archive. */
  private final File archive;

  /** The name of the entry in the archive. */
  private final String entryName;

  /** The contents of the entry. */
  private final byte[] contents;

  /**
   * Constructs a {@code AnnotationFileResource} for the specified entry in the specified archive.
   *
   * @param archive the JAR or ZIP archive
   * @param entryName the name of the entry in the archive
   * @param contents the contents of the entry
   */
  public JarEntryAnnotationFileResource(File archive, String entryName, byte[] contents) {
    this.archive = archive;
    this.entryName = entryName;
    this.contents = contents;
  }

  @Override
  public String getDescription() {
    return archive.getPath() + "!" + entryName;
  }

  @Override
  public InputStream getInputStream() {
    return new ByteArrayInputStream(contents);
  }
}
