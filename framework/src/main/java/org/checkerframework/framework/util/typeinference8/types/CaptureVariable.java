package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;

/** Variables created as a part of a capture bound. */
@Interned public class CaptureVariable extends Variable {

  /**
   * Creates a captured variable
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
    super(type, typeVariableJava, invocation, context, map, context.getNextCaptureVariableId());
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
   * Returns the constraints generated when incorporating a capture bound. See JLS 18.3.2.
   *
   * @param Ai the captured type argument
   * @param Bi the bound of the type variable
   * @return constraints generated when incorporating a capture bound
   */
  public ConstraintSet getWildcardConstraints(AbstractType Ai, AbstractType Bi) {
    return variableBounds.getWildcardConstraints(Ai, Bi);
  }

  @Override
  public boolean isCaptureVariable() {
    return true;
  }
}
