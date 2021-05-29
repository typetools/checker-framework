package org.checkerframework.framework.stub;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/** {@link File}-based implementation of {@link AnnotationFileResource}. */
public class FileAnnotationFileResource implements AnnotationFileResource {
  /** The underlying file. */
  private final File file;

  /**
   * Constructs a {@code AnnotationFileResource} for the specified annotation file (stub file or
   * ajava file).
   *
   * @param file the annotation file
   */
  public FileAnnotationFileResource(File file) {
    this.file = file;
  }

  @Override
  public String getDescription() {
    return file.getAbsolutePath();
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return new FileInputStream(file);
  }
}
