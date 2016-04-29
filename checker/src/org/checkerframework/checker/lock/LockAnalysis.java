package org.checkerframework.checker.lock;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.Pair;

import java.util.List;

import javax.lang.model.element.VariableElement;

/**
 * The analysis class for the lock type system (serves as factory for the
 * transfer function, stores and abstract values).
 */
public class LockAnalysis extends
        CFAbstractAnalysis<CFValue, LockStore, LockTransfer> {

    public LockAnalysis(BaseTypeChecker checker,
            LockAnnotatedTypeFactory factory,
            List<Pair<VariableElement, CFValue>> fieldValues) {
        super(checker, factory, fieldValues);
    }

    @Override
    public LockTransfer createTransferFunction() {
        return new LockTransfer(this,(LockChecker)checker);
    }

    @Override
    public LockStore createEmptyStore(boolean sequentialSemantics) {
        return new LockStore(this, sequentialSemantics);
    }

    @Override
    public LockStore createCopiedStore(LockStore s) {
        return new LockStore(this, s);
    }

    @Override
    public CFValue createAbstractValue(AnnotatedTypeMirror type) {
        return defaultCreateAbstractValue(this, type);
    }
}
