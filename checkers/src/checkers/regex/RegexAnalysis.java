package checkers.regex;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;

import javacutils.Pair;

import dataflow.analysis.checkers.CFAbstractAnalysis;
import dataflow.analysis.checkers.CFStore;
import dataflow.analysis.checkers.CFValue;

import checkers.types.AnnotatedTypeMirror;

public class RegexAnalysis extends
        CFAbstractAnalysis<CFValue, CFStore, RegexTransfer> {

    public RegexAnalysis(RegexAnnotatedTypeFactory factory,
            ProcessingEnvironment env, RegexChecker checker,
            List<Pair<VariableElement, CFValue>> fieldValues) {
        super(factory, env, checker, fieldValues);
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
        return new CFValue(this, type);
    }
}
