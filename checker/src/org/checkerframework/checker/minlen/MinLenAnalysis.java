package org.checkerframework.checker.minlen;

import java.util.List;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.Pair;

// subclasses the base analysis to use our Transfers instead of the deafaults

public class MinLenAnalysis extends CFAbstractAnalysis<MinLenValue, MinLenStore, MinLenTransfer> {
    MinLenAnnotatedTypeFactory atypeFactory;

    public MinLenAnalysis(
            BaseTypeChecker checker,
            MinLenAnnotatedTypeFactory factory,
            List<Pair<VariableElement, MinLenValue>> fieldValues) {
        super(checker, factory, fieldValues);
        this.atypeFactory = (MinLenAnnotatedTypeFactory) super.atypeFactory;
    }

    //overrides the superclass method to return our transfers
    @Override
    public MinLenTransfer createTransferFunction() {
        return new MinLenTransfer(this);
    }

    @Override
    public @Nullable MinLenValue createAbstractValue(AnnotatedTypeMirror type) {
        if (!AnnotatedTypes.isValidType(qualifierHierarchy, type)) {
            // If the type is not valid, we return null, which is the same as
            // 'no information'.
            return null;
        }
        return new MinLenValue(this, type);
    }

    @Override
    public MinLenStore createCopiedStore(MinLenStore s) {
        return new MinLenStore(s);
    }

    @Override
    public MinLenStore createEmptyStore(boolean sequentialSemantics) {
        return new MinLenStore(this, sequentialSemantics);
    }
}
