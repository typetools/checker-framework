package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import javax.lang.model.type.TypeVariable;

public interface Variable extends AbstractType {

    @Override
    TypeVariable getJavaType();

    VariableBounds getBounds();

    /**
     * Adds the initial bounds to this variable. These are the bounds implied by the upper bounds of
     * the type variable. See end of JLS 18.1.3.
     *
     * @param map used to determine if the bounds refer to another variable
     */
    void initialBounds(Theta map);

    ExpressionTree getInvocation();

    /** @return true if this is a variable created for a capture bound. */
    default boolean isCaptureVariable() {
        return false;
    }

    /** Save the current bounds. */
    void save();

    /** Restore the bounds to the state previously saved. */
    void restore();
}
