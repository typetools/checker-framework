package org.checkerframework.javacutil.typeinference8.typemirror.type;

import com.sun.source.tree.ExpressionTree;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.javacutil.typeinference8.constraint.ConstraintSet;
import org.checkerframework.javacutil.typeinference8.types.AbstractType;
import org.checkerframework.javacutil.typeinference8.types.CaptureVariable;
import org.checkerframework.javacutil.typeinference8.util.Java8InferenceContext;

/** A variable created for a capture bound. */
public class CaptureVariableTypeMirror extends VariableTypeMirror implements CaptureVariable {

    CaptureVariableTypeMirror(
            TypeVariable type, ExpressionTree invocation, Java8InferenceContext context) {
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
