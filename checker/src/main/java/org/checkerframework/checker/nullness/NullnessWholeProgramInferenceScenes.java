package org.checkerframework.checker.nullness;

import org.checkerframework.common.wholeprograminference.WholeProgramInferenceScenes;
import org.checkerframework.framework.type.AnnotatedTypeFactory;

/** A WholeProgramInferenceScenes customized for the Nullness Checker. */
public class NullnessWholeProgramInferenceScenes extends WholeProgramInferenceScenes {

    /**
     * Create a NullnessWholeProgramInferenceScenes.
     *
     * @param atypeFactory the associated type factory
     */
    public NullnessWholeProgramInferenceScenes(AnnotatedTypeFactory atypeFactory) {
        // "false" argument means don't ignore null assignments.
        super(atypeFactory, false);
    }
}
