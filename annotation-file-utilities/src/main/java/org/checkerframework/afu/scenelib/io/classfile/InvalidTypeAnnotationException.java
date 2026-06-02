package org.checkerframework.afu.scenelib.io.classfile;

/**
 * An {@code InvalidTypeAnnotationException} indicates that an extended annotation was created with
 * invalid information. For example, an extended annotation on a local variable should not contain
 * offset information.
 */
public class InvalidTypeAnnotationException extends RuntimeException {
  static final long serialVersionUID = 20060712L; // Today's date.

  /**
   * Constructs a new {@code InvalidTypeAnnotationException} with the given error message.
   *
   * @param msg a message describing what was wrong with the extended annotation
   */
  public InvalidTypeAnnotationException(String msg) {
    super(msg);
  }
}
