package org.checkerframework.framework.util.typeinference8;

import com.sun.source.util.TreePath;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.javacutil.typeinference8.types.InferenceFactory;
import org.checkerframework.javacutil.typeinference8.util.Java8InferenceContext;

public class CFInferenceContext extends Java8InferenceContext {

    final AnnotatedTypeFactory typeFactory;

    public CFInferenceContext(
            AnnotatedTypeFactory factory,
            TreePath pathToExpression,
            CFInvocationTypeInference cfInvocationTypeInference) {
        super(
                factory.getProcessingEnv(),
                factory.getContext().getTypeUtils(),
                pathToExpression,
                cfInvocationTypeInference);
        this.typeFactory = factory;
    }

    @Override
    public InferenceFactory createInferenceFactory() {
        return new InferenceAnnotatedFactory(this);
    }
}
