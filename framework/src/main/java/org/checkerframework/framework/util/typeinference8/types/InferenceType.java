package org.checkerframework.framework.util.typeinference8.types;

import java.util.List;

public interface InferenceType extends AbstractType {

    @Override
    default Kind getKind() {
        return Kind.INFERENCE_TYPE;
    }

    List<AbstractType> getTypeBounds();
}
