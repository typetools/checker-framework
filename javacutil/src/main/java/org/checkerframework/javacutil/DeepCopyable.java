package org.checkerframework.javacutil;

/**
 * An interface for types that implement the {@code deepCopy()} method.
 *
 * @param <T> the type of the subtype of DeepCopyable
 */
public interface DeepCopyable<T> extends Cloneable {

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
  T deepCopy();

  /**
   * Returns the deep copy of a non-null argument and {@code null} for a {@code null} argument.
   *
   * @param object object to copy
   * @return the deep copy of a non-null argument and {@code null} for a {@code null} argument
   * @param <T> the type of the object
   */
  static <T extends DeepCopyable<T>> T deepCopyOrNull(T object) {
    if (object == null) {
      return null;
    }
    return object.deepCopy();
  }
}
