package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;

/** Variables created as a part of a capture bound. */
@Interned public class CaptureVariable extends Variable {

  /**
   * Creates a captured variable.
   *
   * @param type the annotated type variable that is captured
   * @param typeVariableJava the type variable that is captured
   * @param invocation invocation expression for the variable
   * @param context the context
   * @param map a mapping from type variable to inference variable
   */
  CaptureVariable(
      AnnotatedTypeVariable type,
      TypeVariable typeVariableJava,
      ExpressionTree invocation,
      Java8InferenceContext context,
      Theta map) {
    this(type, typeVariableJava, invocation, context, map, context.getNextCaptureVariableId());
  }

  /**
   * Creates a captured variable with the given id.
   *
   * @param type the annotated type variable that is captured
   * @param typeVariableJava the type variable that is captured
   * @param invocation invocation expression for the variable
   * @param context the context
   * @param map a mapping from type variable to inference variable
   * @param id a unique number for this variable
   */
  CaptureVariable(
      AnnotatedTypeVariable type,
      TypeVariable typeVariableJava,
      ExpressionTree invocation,
      Java8InferenceContext context,
      Theta map,
      int id) {
    super(type, typeVariableJava, invocation, context, map, id);
  }

  @Override
  public String toString() {
    return String.format("captured %s from %s", typeVariableJava, invocation);

    // Uncomment for easier to read names for debugging.
    //    // Use "b" instead of "a" like super so it is apparent that this is a capture variable.
    //    if (variableBounds.hasInstantiation()) {
    //      return "b" + id + " := " + variableBounds.getInstantiation();
    //    }
    //    return "b" + id;
  }

  /**
   * Returns the constraints generated when incorporating a capture bound, or null if the
   * incorporation implies the bound false. See JLS 18.3.2.
   *
   * @param Ai the captured type argument
   * @param Bi the bound of the type variable
   * @return constraints generated when incorporating a capture bound, or null if the bound false is
   *     implied
   */
  public @Nullable ConstraintSet getWildcardConstraints(AbstractType Ai, AbstractType Bi) {
    return variableBounds.getWildcardConstraints(Ai, Bi);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Two capture variables are equal only if they are the same object. Every capture bound
   * introduces fresh capture variables (see JLS 18.3.2), and the mapping created by {@link
   * InferenceFactory#createThetaForCapture} is not cached, so two capture bounds for the same
   * invocation create distinct capture variables for the same type variable. Those variables stand
   * for different types, so {@link Variable#equals}, which compares the type variable and the
   * invocation, must not be used for them. Reference equality is also what the {@code @Interned}
   * annotation on this class requires.
   *
   * <p>{@link Variable#hashCode} is still correct for capture variables: it may return the same
   * value for two capture variables that are not equal, which is permitted.
   */
  @Override
  public boolean equals(Object o) {
    return this == o;
  }

  /** True if the type argument that this variable captures is a wildcard. */
  private boolean capturedWildcard = true;

  /**
   * Sets whether the type argument that this variable captures is a wildcard.
   *
   * @param capturedWildcard true if the captured type argument is a wildcard
   */
  public void setCapturedWildcard(boolean capturedWildcard) {
    this.capturedWildcard = capturedWildcard;
  }

  @Override
  public boolean isCaptureVariable() {
    return true;
  }

  @Override
  public boolean isCapturedWildcard() {
    return capturedWildcard;
  }
}
