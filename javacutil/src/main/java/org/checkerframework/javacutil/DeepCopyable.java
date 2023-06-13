package org.checkerframework.javacutil;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

/**
 * An interface for types that implement the {@code deepCopy()} method.
 *
 * @param <T> the type of the subtype of DeepCopyable
 * @deprecated use org.plumelib.util.DeepCopyable
 */
@Deprecated // 2023-06-02
public interface DeepCopyable<T> {

  /**
   * Returns a deep copy of this. A deep copy is equal to the original, but side effects to either
   * object are not visible in the other. A deep copy may share immutable state with the original.
   *
   * <p>The run-time class of the result is identical to the run-time class of this. The deep copy
   * is equal to {@code this} (per {@code equals()} if the object's class does not use reference
   * equality as {@code Object.equals()} does).
   *
   * @return a deep copy of this
   */
  T deepCopy();

  /**
   * Returns the deep copy of a non-null argument, or {@code null} for a {@code null} argument.
   *
   * @param object object to copy
   * @return the deep copy of a non-null argument, or {@code null} for a {@code null} argument
   * @param <T2> the type of the object
   */
  static <T2 extends @Nullable DeepCopyable<T2>> @PolyNull T2 deepCopyOrNull(@PolyNull T2 object) {
    if (object == null) {
      return null;
    }
    return object.deepCopy();
  }
}
