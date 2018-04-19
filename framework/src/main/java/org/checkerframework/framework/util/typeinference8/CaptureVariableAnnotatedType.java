package org.checkerframework.framework.util.typeinference8;

import com.sun.source.tree.ExpressionTree;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.javacutil.typeinference8.constraint.ConstraintSet;
import org.checkerframework.javacutil.typeinference8.types.AbstractType;
import org.checkerframework.javacutil.typeinference8.types.CaptureVariable;

public class CaptureVariableAnnotatedType extends VariableAnnotatedType implements CaptureVariable {

    CaptureVariableAnnotatedType(
            AnnotatedTypeVariable type, ExpressionTree invocation, CFInferenceContext context) {
        super(type, invocation, context, context.getNextCaptureVariableId());
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
    @Override
    public ConstraintSet getWildcardConstraints(AbstractType Ai, AbstractType Bi) {
        return variableBounds.getWildcardConstraints(Ai, Bi);
    }

    @Override
    public boolean isCaptureVariable() {
        return true;
    }
}
