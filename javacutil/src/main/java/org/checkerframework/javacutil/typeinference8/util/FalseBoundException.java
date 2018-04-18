package org.checkerframework.javacutil.typeinference8.util;

import org.checkerframework.javacutil.typeinference8.constraint.Constraint;

public class FalseBoundException extends RuntimeException {
    private static final long serialVersionUID = 1;

    public FalseBoundException(Constraint constraint) {
        super("Constraint: " + constraint);
    }
}
