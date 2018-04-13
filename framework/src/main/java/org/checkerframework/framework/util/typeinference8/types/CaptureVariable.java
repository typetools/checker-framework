package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.util.typeinference8.constraint.Constraint;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.constraint.Typing;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;

/** A variable created for a capture bound. */
public class CaptureVariable extends Variable {

    public CaptureVariable(
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
    public ConstraintSet getWildcardConstraints(AbstractType Ai, AbstractType Bi) {
        ConstraintSet constraintSet = new ConstraintSet();

        // Only concerned with bounds against proper types or inference types.
        List<AbstractType> upperBoundsNonVar = new ArrayList<>();
        for (AbstractType bound : variableBounds.bounds.get(VariableBounds.BoundKind.UPPER)) {
            if (bound.isProper() || bound.isInferenceType()) {
                upperBoundsNonVar.add(bound);
            }
        }
        List<AbstractType> lowerBoundsNonVar = new ArrayList<>();
        for (AbstractType bound : variableBounds.bounds.get(VariableBounds.BoundKind.LOWER)) {
            if (bound.isProper() || bound.isInferenceType()) {
                lowerBoundsNonVar.add(bound);
            }
        }

        for (AbstractType bound : variableBounds.bounds.get(VariableBounds.BoundKind.EQUAL)) {
            if (bound.isProper() || bound.isInferenceType()) {
                // var = R implies the bound false
                return null;
            }
        }

        if (Ai.isUnboundWildcard()) {
            // R <: var implies the bound false
            if (!lowerBoundsNonVar.isEmpty()) {
                return null;
            }
            // var <: R implies the constraint formula <Bi theta <: R>
        } else if (Ai.isUpperBoundedWildcard()) {
            // R <: var implies the bound false
            if (!lowerBoundsNonVar.isEmpty()) {
                return null;
            }
            AbstractType T = Ai.getWildcardUpperBound();
            if (Bi.isObject()) {
                // If Bi is Object, then var <: R implies the constraint formula <T <: R>
                for (AbstractType r : upperBoundsNonVar) {
                    constraintSet.add(new Typing(T, r, Constraint.Kind.SUBTYPE));
                }
            } else if (T.isObject()) {
                // If T is Object, then var <: R implies the constraint formula <Bi theta <: R>
                for (AbstractType r : upperBoundsNonVar) {
                    constraintSet.add(new Typing(Bi, r, Constraint.Kind.SUBTYPE));
                }
            }
            // else no constraint
        } else {
            // Super bounded wildcard
            // var <: R implies the constraint formula <Bi theta <: R>
            for (AbstractType r : upperBoundsNonVar) {
                constraintSet.add(new Typing(Bi, r, Constraint.Kind.SUBTYPE));
            }

            // R <: var implies the constraint formula <R <: T>
            AbstractType T = Ai.getWildcardLowerBound();
            for (AbstractType r : lowerBoundsNonVar) {
                constraintSet.add(new Typing(r, T, Constraint.Kind.SUBTYPE));
            }
        }
        return constraintSet;
    }
}
