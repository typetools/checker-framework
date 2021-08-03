package org.checkerframework.checker.nullness;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;

import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;

/** Boilerplate code to glue together all the parts the KeyFor dataflow classes. */
public class KeyForAnalysis extends CFAbstractAnalysis<KeyForValue, KeyForStore, KeyForTransfer> {

    /**
     * Creates a new {@code KeyForAnalysis}.
     *
     * @param checker the checker
     * @param factory the factory
     */
    public KeyForAnalysis(BaseTypeChecker checker, KeyForAnnotatedTypeFactory factory) {
        super(checker, factory);
    }

    @Override
    public KeyForStore createEmptyStore(boolean sequentialSemantics) {
        return new KeyForStore(this, sequentialSemantics);
    }

    @Override
    public KeyForStore createCopiedStore(KeyForStore store) {
        return new KeyForStore(store);
    }

    @Override
    public KeyForValue createAbstractValue(
            Set<AnnotationMirror> annotations, TypeMirror underlyingType) {

        if (!CFAbstractValue.validateSet(annotations, underlyingType, qualifierHierarchy)) {
            return null;
        }
        return new KeyForValue(this, annotations, underlyingType);
    }
}
