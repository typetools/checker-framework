package checkers.flow.analysis.checkers;

import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;

import checkers.flow.analysis.checkers.CFAbstractValue.InferredAnnotation;
import checkers.regex.RegexAnnotatedTypeFactory;
import checkers.regex.RegexChecker;
import checkers.util.Pair;

public class RegexAnalysis extends
        CFAbstractAnalysis<CFValue, CFStore, RegexTransfer> {

    public RegexAnalysis(RegexAnnotatedTypeFactory factory,
            ProcessingEnvironment env, RegexChecker checker,
            List<Pair<VariableElement, CFValue>> fieldValues) {
        super(factory, env, checker, fieldValues);
    }

    @Override
    protected RegexTransfer createTransferFunction() {
        return new RegexTransfer(this);
    }

    @Override
    protected CFStore createEmptyStore(boolean sequentialSemantics) {
        return new CFStore(this, sequentialSemantics);
    }

    @Override
    protected CFStore createCopiedStore(CFStore s) {
        return new CFStore(this, s);
    }

    @Override
    protected/* @Nullable */CFValue createAbstractValue(
            Set<AnnotationMirror> annotations) {
        return CFAnalysis.defaultCreateAbstractValue(annotations, this);
    }

    @Override
    protected CFValue createAbstractValue(InferredAnnotation[] annotations) {
        return CFAnalysis.defaultCreateAbstractValue(annotations, this);
    }
}
