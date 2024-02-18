package org.checkerframework.javacutil;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.plumelib.util.UtilPlume;

// The class type variables are called V1 and V2 so that T1 and T2 can be used for method type
// variables.
/**
 * Immutable pair class.
 *
 * @param <V1> the type of the first element of the pair
 * @param <V2> the type of the second element of the pair
 * @deprecated use org.plumelib.util.IPair
 */
@Deprecated // 2023-06-02
// TODO: as class is immutable, use @Covariant annotation.
public class Pair<V1, V2> {
  /** The first element of the pair. */
  public final V1 first;

  /** The second element of the pair. */
  public final V2 second;

  private Pair(V1 first, V2 second) {
    this.first = first;
    this.second = second;
  }

  public static <T1, T2> Pair<T1, T2> of(T1 first, T2 second) {
    return new Pair<>(first, second);
  }

  // The typical way to make a copy is to first call super.clone() and then modify it.
  // That implementation strategy does not work for Pair because its fields are final, so the
  // clone and deepCopy() methods use of() instead.

  /**
   * Returns a copy of this in which each element is a clone of the corresponding element of this.
   * {@code clone()} may or may not itself make a deep copy of the elements.
   *
   * @param <T1> the type of the first element of the pair
   * @param <T2> the type of the second element of the pair
   * @param orig a pair
   * @return a copy of {@code orig}, with all elements cloned
   */
  // This method is static so that the pair element types can be constrained to be Cloneable.
  @SuppressWarnings("nullness") // generics problem with deepCopy()
  public static <T1 extends Cloneable, T2 extends Cloneable> Pair<T1, T2> cloneElements(
      Pair<T1, T2> orig) {

    T1 oldFirst = orig.first;
    T1 newFirst = oldFirst == null ? oldFirst : UtilPlume.clone(oldFirst);
    T2 oldSecond = orig.second;
    T2 newSecond = oldSecond == null ? oldSecond : UtilPlume.clone(oldSecond);
    return of(newFirst, newSecond);
  }

  /**
   * Returns a deep copy of this: each element is a deep copy (according to the {@code DeepCopyable}
   * interface) of the corresponding element of this.
   *
   * @param <T1> the type of the first element of the pair
   * @param <T2> the type of the second element of the pair
   * @param orig a pair
   * @return a deep copy of {@code orig}
   */
  @SuppressWarnings("nullness") // generics problem with deepCopy()
  // This method is static so that the pair element types can be constrained to be DeepCopyable.
  public static <T1 extends DeepCopyable<T1>, T2 extends DeepCopyable<T2>> Pair<T1, T2> deepCopy(
      Pair<T1, T2> orig) {
    return of(DeepCopyable.deepCopyOrNull(orig.first), DeepCopyable.deepCopyOrNull(orig.second));
  }

  /**
   * Returns a copy, where the {@code first} element is deep: the {@code first} element is a deep
   * copy (according to the {@code DeepCopyable} interface), and the {@code second} element is
   * identical to the argument.
   *
   * @param <T1> the type of the first element of the pair
   * @param <T2> the type of the second element of the pair
   * @param orig a pair
   * @return a copy of {@code orig}, where the first element is a deep copy
   */
  @SuppressWarnings("nullness") // generics problem with deepCopy()
  public static <T1 extends DeepCopyable<T1>, T2> Pair<T1, T2> deepCopyFirst(Pair<T1, T2> orig) {
    return of(DeepCopyable.deepCopyOrNull(orig.first), orig.second);
  }

  /**
   * Returns a copy, where the {@code second} element is deep: the {@code first} element is
   * identical to the argument, and the {@code second} element is a deep copy (according to the
   * {@code DeepCopyable} interface).
   *
   * @param <T1> the type of the first element of the pair
   * @param <T2> the type of the second element of the pair
   * @param orig a pair
   * @return a copy of {@code orig}, where the second element is a deep copy
   */
  @SuppressWarnings("nullness") // generics problem with deepCopy()
  public static <T1, T2 extends DeepCopyable<T2>> Pair<T1, T2> deepCopySecond(Pair<T1, T2> orig) {
    return of(orig.first, DeepCopyable.deepCopyOrNull(orig.second));
  }

  @Override
  @Pure
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Pair)) {
      return false;
    }
    // generics are not checked at run time!
    @SuppressWarnings("unchecked")
    Pair<V1, V2> other = (Pair<V1, V2>) obj;
    return Objects.equals(this.first, other.first) && Objects.equals(this.second, other.second);
  }

  /** The cached hash code. -1 means it needs to be computed. */
  private int hashCode = -1;

  @Pure
  @Override
  public int hashCode() {
    if (hashCode == -1) {
      hashCode = Objects.hash(first, second);
    }
    return hashCode;
  }

  @SideEffectFree
  @Override
  public String toString() {
    return "Pair(" + first + ", " + second + ")";
  }
}
