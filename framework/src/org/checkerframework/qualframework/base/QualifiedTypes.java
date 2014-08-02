package org.checkerframework.qualframework.base;

import java.util.*;

import com.sun.source.tree.ExpressionTree;

import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedExecutableType;

/** Helper functions for various manipulations of {@link QualifiedTypeMirror}s.
 */
public interface QualifiedTypes<Q> {
    /**
     * Returns the method parameters for the invoked method, with the same number
     * of arguments passed in the methodInvocation tree.
     *
     * If the invoked method is not a vararg method or it is a vararg method
     * but the invocation passes an array to the vararg parameter, it would simply
     * return the method parameters.
     *
     * Otherwise, it would return the list of parameters as if the vararg is expanded
     * to match the size of the passed arguments.
     *
     * @param method the method's type
     * @param args the arguments to the method invocation
     * @return  the types that the method invocation arguments need to be subtype of
     */
    public List<QualifiedTypeMirror<Q>> expandVarArgs(
            QualifiedExecutableType<Q> method,
            List<? extends ExpressionTree> args);
}
