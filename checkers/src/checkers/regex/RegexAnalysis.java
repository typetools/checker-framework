package checkers.regex;

import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;

import checkers.flow.analysis.checkers.CFAbstractAnalysis;
import checkers.flow.analysis.checkers.CFAbstractValue.InferredAnnotation;
import checkers.flow.analysis.checkers.CFAnalysis;
import checkers.flow.analysis.checkers.CFStore;
import checkers.flow.analysis.checkers.CFValue;
import checkers.util.Pair;

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
    public/* @Nullable */CFValue createAbstractValue(
            Set<AnnotationMirror> annotations) {
        return CFAnalysis.defaultCreateAbstractValue(annotations, this);
    }

    @Override
    public CFValue createAbstractValue(InferredAnnotation[] annotations) {
        return CFAnalysis.defaultCreateAbstractValue(annotations, this);
    }
}
