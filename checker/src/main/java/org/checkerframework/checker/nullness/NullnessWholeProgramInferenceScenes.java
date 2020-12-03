package org.checkerframework.checker.nullness;

import org.checkerframework.common.wholeprograminference.WholeProgramInferenceScenes;
import org.checkerframework.framework.type.AnnotatedTypeFactory;

public class NullnessWholeProgramInferenceScenes extends WholeProgramInferenceScenes {

    /** Create a NullnessWholeProgramInferenceScenes. */
    public NullnessWholeProgramInferenceScenes(AnnotatedTypeFactory atypeFactory) {
        // "false" argument means don't ignore null assignments.
        super(atypeFactory, false);
    }
}
