package org.checkerframework.checker.nullness;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.Pair;

import java.util.List;

import javax.lang.model.element.VariableElement;

/**
 * The analysis class for the KeyFor type system (serves as factory for the
 * transfer function, stores and abstract values).
 */
public class KeyForAnalysis extends
    CFAbstractAnalysis<CFValue, CFStore, KeyForTransfer> {

    public KeyForAnalysis(BaseTypeChecker checker,
            KeyForAnnotatedTypeFactory factory,
            List<Pair<VariableElement, CFValue>> fieldValues) {
        super(checker, factory, fieldValues);
    }

    @Override
    public KeyForTransfer createTransferFunction() {
        return new KeyForTransfer(this,(KeyForSubchecker)checker);
    }

    @Override
    public CFStore createEmptyStore(boolean sequentialSemantics) {
        return new CFStore(this, sequentialSemantics);
    }

    @Override
    public CFStore createCopiedStore(CFStore s) {
        return new CFStore(this, s);
    }

    @Override
    public CFValue createAbstractValue(AnnotatedTypeMirror type) {
        return defaultCreateAbstractValue(this, type);
    }
}