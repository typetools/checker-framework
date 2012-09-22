package checkers.nonnull;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;

import checkers.flow.analysis.checkers.CFAbstractAnalysis;
import checkers.flow.analysis.checkers.CFValue;
import checkers.initialization.InitializationStore;
import checkers.types.AnnotatedTypeMirror;
import checkers.util.Pair;

/**
 * The analysis class for the non-null type system (serves as factory for the
 * transfer function, stores and abstract values.
 *
 * @author Stefan Heule
 */
public class NonNullAnalysis extends
        CFAbstractAnalysis<CFValue, InitializationStore, NonNullTransfer> {

    public NonNullAnalysis(NonNullAnnotatedTypeFactory factory,
            ProcessingEnvironment env, AbstractNonNullChecker checker,
            List<Pair<VariableElement, CFValue>> fieldValues) {
        super(factory, env, checker, fieldValues);
    }

    @Override
    public InitializationStore createEmptyStore(boolean sequentialSemantics) {
        return new InitializationStore(this, sequentialSemantics);
    }

    @Override
    public InitializationStore createCopiedStore(InitializationStore s) {
        return new InitializationStore(s);
    }

    @Override
    public CFValue createAbstractValue(AnnotatedTypeMirror type) {
        return new CFValue(this, type);
    }
}
