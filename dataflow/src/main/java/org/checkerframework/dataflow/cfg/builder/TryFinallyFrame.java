package org.checkerframework.dataflow.cfg.builder;

import java.util.Set;
import javax.lang.model.type.TypeMirror;

/** A TryFinallyFrame applies to exceptions of any type. */
class TryFinallyFrame implements TryFrame {
    /** The finally label. */
    protected final Label finallyLabel;

    /**
     * Construct a TryFinallyFrame.
     *
     * @param finallyLabel finally label
     */
    public TryFinallyFrame(Label finallyLabel) {
        this.finallyLabel = finallyLabel;
    }

    @Override
    public String toString() {
        return "TryFinallyFrame: finallyLabel: " + finallyLabel;
    }

    @Override
    public boolean possibleLabels(TypeMirror thrown, Set<Label> labels) {
        labels.add(finallyLabel);
        return true;
    }
}
