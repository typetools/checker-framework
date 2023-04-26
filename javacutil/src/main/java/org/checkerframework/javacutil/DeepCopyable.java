package org.checkerframework.javacutil;

/** An interface for types that implement the {@code deepCopy()} method. */
public interface DeepCopyable extends Cloneable {

  /**
   * Returns a deep copy of this. A deep copy is equal to the original, but side effects to either
   * object are not visible in the other. A deep copy may share immutable state with the original.
   *
   * <p>The run-time class of the result is identical to the run-time class of this. The deep copy
   * is equal to this (per {@code equals()} if the object's class does not use reference equality as
   * {@code Object.equals()} does).
   *
   * @return a deep copy of this
   */
  Object deepCopy();
}
