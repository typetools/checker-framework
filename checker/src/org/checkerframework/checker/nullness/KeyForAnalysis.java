package org.checkerframework.checker.nullness;

import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.javacutil.Pair;

/** Boiler plate code to glue together all the parts the KeyFor dataflow classes. */
public class KeyForAnalysis extends CFAbstractAnalysis<KeyForValue, KeyForStore, KeyForTransfer> {

    public KeyForAnalysis(
            BaseTypeChecker checker,
            KeyForAnnotatedTypeFactory factory,
            List<Pair<VariableElement, KeyForValue>> fieldValues,
            int maxCountBeforeWidening) {
        super(checker, factory, fieldValues, maxCountBeforeWidening);
    }

    public KeyForAnalysis(
            BaseTypeChecker checker,
            KeyForAnnotatedTypeFactory factory,
            List<Pair<VariableElement, KeyForValue>> fieldValues) {
        super(checker, factory, fieldValues);
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
