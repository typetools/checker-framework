package org.checkerframework.framework.util.typeinference8.util;

import org.checkerframework.framework.util.typeinference8.bound.FalseBound;
import org.checkerframework.framework.util.typeinference8.constraint.Constraint;

public class FalseBoundException extends RuntimeException {
    private static final long serialVersionUID = 1;
    private final boolean annotatedTypeFailed;

    public FalseBoundException(Constraint constraint, FalseBound result) {
        super("Constraint: " + constraint);
        annotatedTypeFailed = result.isAnnotatedTypeFailure();
    }

    public boolean isAnnotatedTypeFailed() {
        return annotatedTypeFailed;
    }
}
