package org.checkerframework.framework.util.typeinference8.util;

import org.checkerframework.framework.util.typeinference8.constraint.Constraint;

public class FalseBoundException extends RuntimeException {
    static final long serialVersionUID = 1;

    public FalseBoundException(Constraint constraint) {
        super("Constraint: " + constraint);
    }
}
