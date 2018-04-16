package org.checkerframework.framework.util.typeinference8.types;

import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;

public interface CaptureVariable extends AbstractType, Variable {

    /** These are constraints generated when incorporating a capture bound. See JLS 18.3.2. */
    ConstraintSet getWildcardConstraints(AbstractType Ai, AbstractType Bi);

    @Override
    default boolean isCaptureVariable() {
        return true;
    }
}
