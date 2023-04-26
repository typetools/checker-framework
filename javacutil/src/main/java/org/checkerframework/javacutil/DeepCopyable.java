package org.checkerframework.javacutil;

/**
 * An interface for types that implement the {@code deepCopy()} method.
 *
 * @param <T>
 */
public interface DeepCopyable<T> extends Cloneable {

  /**
   * Returns a deep copy of this. The run-time class of the result is identical to the run-time
   * class of this.
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
