package checkers.flow.analysis.checkers;

import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import checkers.regex.RegexAnnotatedTypeFactory;
import checkers.regex.RegexChecker;

public class RegexAnalysis extends
        CFAbstractAnalysis<CFValue, CFStore, RegexTransfer> {

    public RegexAnalysis(RegexAnnotatedTypeFactory factory,
            ProcessingEnvironment env, RegexChecker checker) {
        super(factory, env, checker);
    }

    @Override
    protected RegexTransfer createTransferFunction() {
        return new RegexTransfer(this);
    }

    @Override
    protected CFStore createEmptyStore() {
        return new CFStore(this);
    }

    @Override
    protected CFStore createCopiedStore(CFStore s) {
        return new CFStore(this, s);
    }

    @Override
    protected/* @Nullable */CFValue createAbstractValue(
            Set<AnnotationMirror> annotations) {
        return CFAnalysis.defaultCreateAbstractValue(annotations,
                supportedAnnotations, this);
    }

}
