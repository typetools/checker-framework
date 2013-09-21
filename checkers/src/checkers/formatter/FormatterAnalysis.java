package checkers.formatter;

import checkers.flow.CFAbstractAnalysis;
import checkers.flow.CFStore;
import checkers.flow.CFValue;
import checkers.types.AnnotatedTypeMirror;

import javacutils.Pair;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;

/**
 * Needed for flow analysis, to implement the {@link FormatUtil#asFormat} method.
 *
 * @author Konstantin Weitz
 */
public class FormatterAnalysis extends
        CFAbstractAnalysis<CFValue, CFStore, FormatterTransfer> {
    public FormatterAnalysis(FormatterAnnotatedTypeFactory factory,
            ProcessingEnvironment env, FormatterChecker checker,
            List<Pair<VariableElement, CFValue>> fieldValues) {
        super(factory, env, checker, fieldValues);
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
