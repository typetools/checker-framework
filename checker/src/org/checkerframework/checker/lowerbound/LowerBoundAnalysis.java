package org.checkerframework.checker.lowerbound;

import java.util.List;
import javax.lang.model.element.VariableElement;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.Pair;

/**
 * This class doesn't really have anything unique or interesting
 * about it. As far as I can tell, the framework requires that I
 * implement a special version for the lowerbound checker, though, so
 * this is it.
 */
public class LowerBoundAnalysis extends CFAbstractAnalysis<CFValue, CFStore, LowerBoundTransfer> {
    public LowerBoundAnalysis(
            BaseTypeChecker checker,
            LowerBoundAnnotatedTypeFactory factory,
            List<Pair<VariableElement, CFValue>> fieldValues) {
        super(checker, factory, fieldValues);
    }

    @Override
    public LowerBoundTransfer createTransferFunction() {
        return new LowerBoundTransfer(this);
    }

    @Override
    public CFValue createAbstractValue(AnnotatedTypeMirror type) {
        return new CFValue(this, type);
    }

    @Override
    public CFStore createCopiedStore(CFStore s) {
        return new CFStore(this, s);
    }

    @Override
    public CFStore createEmptyStore(boolean sequentialSemantics) {
        return new CFStore(this, sequentialSemantics);
    }
}
