package checkers.nonnull;

import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;

import checkers.commitment.CommitmentStore;
import checkers.flow.analysis.checkers.CFAbstractAnalysis;
import checkers.flow.analysis.checkers.CFAnalysis;
import checkers.flow.analysis.checkers.CFValue;
import checkers.flow.analysis.checkers.CFAbstractValue.InferredAnnotation;
import checkers.util.Pair;

/**
 * The analysis class for the non-null type system (serves as factory for the
 * transfer function, stores and abstract values.
 *
 * @author Stefan Heule
 */
public class NonNullAnalysis extends
        CFAbstractAnalysis<CFValue, CommitmentStore, NonNullTransfer> {

    public NonNullAnalysis(NonNullAnnotatedTypeFactory factory,
            ProcessingEnvironment env, NonNullChecker checker,
            List<Pair<VariableElement, CFValue>> fieldValues) {
        super(factory, env, checker, fieldValues);
    }

    @Override
    protected NonNullTransfer createTransferFunction() {
        return new NonNullTransfer(this);
    }

    @Override
    protected CommitmentStore createEmptyStore(boolean sequentialSemantics) {
        return new CommitmentStore(this, sequentialSemantics);
    }

    @Override
    protected CommitmentStore createCopiedStore(CommitmentStore s) {
        return new CommitmentStore(s);
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
