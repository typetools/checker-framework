package org.checkerframework.javacutil.typeinference8.types;

public interface InferenceType extends AbstractType {

    @Override
    default Kind getKind() {
        return Kind.INFERENCE_TYPE;
    }
}
