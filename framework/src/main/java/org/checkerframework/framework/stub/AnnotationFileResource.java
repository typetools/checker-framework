package org.checkerframework.framework.stub;

import java.io.IOException;
import java.io.InputStream;

/** Interface for sources of stub data. */
public interface AnnotationFileResource {
  /**
   * Returns a user-friendly description of the resource (e.g. a filesystem path).
   *
   * @return a description of the resource
   */
  String getDescription();

  /** Returns a stream for reading the contents of the resource. */
  InputStream getInputStream() throws IOException;
}
