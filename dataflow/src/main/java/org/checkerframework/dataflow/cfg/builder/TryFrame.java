package org.checkerframework.dataflow.cfg.builder;

import java.util.Set;

import javax.lang.model.type.TypeMirror;

/**
 * A TryFrame takes a thrown exception type and maps it to a set of possible control-flow
 * successors.
 */
/*package-private*/ interface TryFrame {
    /**
     * Given a type of thrown exception, add the set of possible control flow successor {@link
     * Label}s to the argument set. Return true if the exception is known to be caught by one of
     * those labels and false if it may propagate still further.
     */
    public boolean possibleLabels(TypeMirror thrown, Set<Label> labels);
}
