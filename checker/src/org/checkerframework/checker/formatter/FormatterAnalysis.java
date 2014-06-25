package org.checkerframework.checker.formatter;

import java.util.List;

import javax.lang.model.element.VariableElement;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.Pair;

/**
 * Needed for flow analysis, to implement the {@link FormatUtil#asFormat} method.
 *
 * @author Konstantin Weitz
 */
public class FormatterAnalysis extends
        CFAbstractAnalysis<CFValue, CFStore, FormatterTransfer> {
    public FormatterAnalysis(BaseTypeChecker checker,
            FormatterAnnotatedTypeFactory factory,
            List<Pair<VariableElement, CFValue>> fieldValues) {
        super(checker, factory, fieldValues);
    }

    @Override
    public FormatterTransfer createTransferFunction() {
        return new FormatterTransfer(this,(FormatterChecker)checker);
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
