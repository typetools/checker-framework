package org.checkerframework.javacutil;

/** An interface for types that implement the {@code deepCopy()} method. */
public interface DeepCopyable extends Cloneable {

  /**
   * Returns a deep copy of this. The run-time class of the result is identical to the run-time
   * class of this.
   *
   * @return a deep copy of this
   */
  Object deepCopy();
}
