package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;

/** Variables created as a part of a capture bound. */
public class CaptureVariable extends Variable {

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
    // Use "b" instead of "a" like super so it is apparent that this is a capture variable.
    if (variableBounds.hasInstantiation()) {
      return "b" + id + " := " + variableBounds.getInstantiation();
    }
    return "b" + id;
  }

  /** These are constraints generated when incorporating a capture bound. See JLS 18.3.2. */
  public ConstraintSet getWildcardConstraints(AbstractType Ai, AbstractType Bi) {
    return variableBounds.getWildcardConstraints(Ai, Bi);
  }

  @Override
  public boolean isCaptureVariable() {
    return true;
  }
}
