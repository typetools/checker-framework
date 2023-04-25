package org.checkerframework.javacutil;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

/** Simple pair class for multiple returns. */
// TODO: as class is immutable, use @Covariant annotation.
public class Pair<V1, V2> {
  /** The first element in the pair. */
  public final V1 first;
  /** The second element in the pair. */
  public final V2 second;

  private Pair(V1 v1, V2 v2) {
    this.first = v1;
    this.second = v2;
  }

  public static <V1, V2> Pair<V1, V2> of(V1 v1, V2 v2) {
    return new Pair<>(v1, v2);
  }

  // The typical way to make a copy is to first call super.clone() and then modify it.
  // That implementation strategy does not work for Pair because its fields are final, so the clone
  // and deepCopy methods use of() instead.

  /**
   * Returns a copy of this in which each element is a clone of the corresponding element of this.
   * {@code clone()} may or may not itself make a deep copy of the elements.
   *
   * @param <V1> the type of the first element of the pair
   * @param <V2> the type of the second element of the pair
   * @param orig a pair
   * @return a copy of {@code orig}, with all elements cloned
   */
  @SuppressWarnings("nullness") // generics problem with deepCopy()
  public static <V1 extends Cloneable, V2 extends Cloneable> Pair<V1, V2> cloneElements(
      Pair<V1, V2> orig) {

    V1 oldFirst = orig.first;
    V1 newFirst = oldFirst == null ? oldFirst : CollectionUtils.clone(oldFirst);
    V2 oldSecond = orig.second;
    V2 newSecond = oldSecond == null ? oldSecond : CollectionUtils.clone(oldSecond);
    return of(newFirst, newSecond);
  }

  /**
   * Returns a deep copy of this: each element is a deep copy of the corresponding element of this.
   *
   * @param <V1> the type of the first element of the pair
   * @param <V2> the type of the second element of the pair
   * @param orig a pair
   * @return a deep copy of {@code orig}
   */
  @SuppressWarnings("nullness") // generics problem with deepCopy()
  public static <V1 extends DeepCopyable, V2 extends DeepCopyable> Pair<V1, V2> deepCopy(
      Pair<V1, V2> orig) {

    V1 oldFirst = orig.first;
    @SuppressWarnings("unchecked")
    V1 newFirst = oldFirst == null ? oldFirst : (V1) oldFirst.deepCopy();
    V2 oldSecond = orig.second;
    @SuppressWarnings("unchecked")
    V2 newSecond = oldSecond == null ? oldSecond : (V2) oldSecond.deepCopy();
    return of(newFirst, newSecond);
  }

  /**
   * Returns a copy, where the {@code first} element is deep: the {@code first} element is a deep
   * copy, and the {@code} second element is identical to the argument.
   *
   * @param <V1> the type of the first element of the pair
   * @param <V2> the type of the second element of the pair
   * @param orig a pair
   * @return a copy of {@code orig}, where the first element is a deep copy
   */
  @SuppressWarnings("nullness") // generics problem with deepCopy()
  public static <V1 extends DeepCopyable, V2> Pair<V1, V2> deepCopyFirst(Pair<V1, V2> orig) {
    // Cannot modify result of super.clone() because fields are final.

    V1 oldFirst = orig.first;
    @SuppressWarnings("unchecked")
    V1 newFirst = oldFirst == null ? oldFirst : (V1) oldFirst.deepCopy();
    V2 oldSecond = orig.second;
    V2 newSecond = oldSecond;
    return of(newFirst, newSecond);
  }

  /**
   * Returns a copy, where the {@code second} element is deep: the {@code first} element is
   * identical to the argument, and the {@code} second element is a deep copy.
   *
   * @param <V1> the type of the first element of the pair
   * @param <V2> the type of the second element of the pair
   * @param orig a pair
   * @return a copy of {@code orig}, where the second element is a deep copy
   */
  @SuppressWarnings("nullness") // generics problem with deepCopy()
  public static <V1, V2 extends DeepCopyable> Pair<V1, V2> deepCopySecond(Pair<V1, V2> orig) {

    V1 oldFirst = orig.first;
    V1 newFirst = oldFirst;
    V2 oldSecond = orig.second;
    @SuppressWarnings("unchecked")
    V2 newSecond = oldSecond == null ? oldSecond : (V2) oldSecond.deepCopy();
    return of(newFirst, newSecond);
  }

  @SideEffectFree
  @Override
  public String toString() {
    return "Pair(" + first + ", " + second + ")";
  }

  private int hashCode = -1;

  @Pure
  @Override
  public int hashCode() {
    if (hashCode == -1) {
      hashCode = Objects.hash(first, second);
    }
    return hashCode;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Pair)) {
      return false;
    }
    @SuppressWarnings("unchecked")
    Pair<V1, V2> other = (Pair<V1, V2>) o;
    return Objects.equals(this.first, other.first) && Objects.equals(this.second, other.second);
  }
}
