package org.checkerframework.checker.guieffect;

import java.lang.annotation.Annotation;
import org.checkerframework.checker.guieffect.qual.PolyUIEffect;
import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

/** An effect -- either UIEffect, PolyUIEffect, or SafeEffect. */
public final class Effect {
  // Colin hates Java's comparable interface, so he's not using it

  private final Class<? extends Annotation> annotClass;

  /**
   * Create a new Effect object.
   *
   * @param cls one of UIEffect.class, PolyUIEffect.class, or SafeEffect.class
   */
  public Effect(Class<? extends Annotation> cls) {
    assert cls == UIEffect.class || cls == PolyUIEffect.class || cls == SafeEffect.class;
    annotClass = cls;
  }

  /**
   * Return true iff {@code left} is less than or equal to {@code right}.
   *
   * @param left the first effect to compare
   * @param right the first effect to compare
   * @return true iff {@code left} is less than or equal to {@code right}
   */
  public static boolean lessThanOrEqualTo(Effect left, Effect right) {
    assert (left != null && right != null);
    boolean leftBottom = left.annotClass == SafeEffect.class;
    boolean rightTop = right.annotClass == UIEffect.class;
    return leftBottom || rightTop || left.annotClass == right.annotClass;
  }

  public static Effect min(Effect l, Effect r) {
    if (lessThanOrEqualTo(l, r)) {
      return l;
    } else {
      return r;
    }
  }

  public static final class EffectRange {
    public final Effect min, max;

    public EffectRange(Effect min, Effect max) {
      assert (min != null || max != null);
      // If one is null, fill in with the other
      this.min = (min != null ? min : max);
      this.max = (max != null ? max : min);
    }
  }

  /**
   * Return true if this is SafeEffect.
   *
   * @return true if this is SafeEffect
   */
  public boolean isSafe() {
    return annotClass == SafeEffect.class;
  }

  /**
   * Return true if this is UIEffect.
   *
   * @return true if this is UIEffect
   */
  public boolean isUI() {
    return annotClass == UIEffect.class;
  }

  /**
   * Return true if this is PolyUIEffect.
   *
   * @return true if this is PolyUIEffect
   */
  @Pure
  public boolean isPoly() {
    return annotClass == PolyUIEffect.class;
  }

  public Class<? extends Annotation> getAnnot() {
    return annotClass;
  }

  @SideEffectFree
  @Override
  public String toString() {
    return annotClass.getSimpleName();
  }

  /**
   * Return true if this equals the given effect.
   *
   * @param e the effect to compare this to
   * @return true if this equals the given effect
   */
  @SuppressWarnings("NonOverridingEquals") // TODO: clean this up!
  public boolean equals(Effect e) {
    return annotClass == e.annotClass;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o instanceof Effect) {
      return this.equals((Effect) o);
    } else {
      return super.equals(o);
    }
  }

  @Pure
  @Override
  public int hashCode() {
    return 31 + annotClass.hashCode();
  }
}
