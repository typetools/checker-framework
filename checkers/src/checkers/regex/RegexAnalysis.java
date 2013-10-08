package checkers.regex;

import checkers.basetype.BaseTypeChecker;
import checkers.flow.CFAbstractAnalysis;
import checkers.flow.CFStore;
import checkers.flow.CFValue;
import checkers.types.AnnotatedTypeMirror;

import javacutils.Pair;

import java.util.List;

import javax.lang.model.element.VariableElement;

public class RegexAnalysis extends
        CFAbstractAnalysis<CFValue, CFStore, RegexTransfer> {

    public RegexAnalysis(BaseTypeChecker checker,
            RegexAnnotatedTypeFactory factory,
            List<Pair<VariableElement, CFValue>> fieldValues) {
        super(checker, factory, fieldValues);
    }

    @Override
    public RegexTransfer createTransferFunction() {
        return new RegexTransfer(this);
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
