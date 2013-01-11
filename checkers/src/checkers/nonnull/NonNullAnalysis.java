package checkers.nonnull;

import java.util.List;

import javacutils.Pair;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;

import checkers.flow.analysis.checkers.CFAbstractAnalysis;
import checkers.flow.analysis.checkers.CFValue;
import checkers.types.AnnotatedTypeMirror;
import checkers.util.AnnotatedTypes;

/**
 * The analysis class for the non-null type system (serves as factory for the
 * transfer function, stores and abstract values.
 *
 * @author Stefan Heule
 */
public class NonNullAnalysis extends
        CFAbstractAnalysis<NonNullValue, NonNullStore, NonNullTransfer> {

    public NonNullAnalysis(NonNullAnnotatedTypeFactory factory,
            ProcessingEnvironment env, AbstractNonNullChecker checker,
            List<Pair<VariableElement, NonNullValue>> fieldValues) {
        super(factory, env, checker, fieldValues);
    }

    @Override
    public NonNullStore createEmptyStore(boolean sequentialSemantics) {
        return new NonNullStore(this, sequentialSemantics);
    }

    @Override
    public NonNullStore createCopiedStore(NonNullStore s) {
        return new NonNullStore(s);
    }

    @Override
    public NonNullValue createAbstractValue(AnnotatedTypeMirror type) {
        if (!AnnotatedTypes.isValidType(qualifierHierarchy, type)) {
            // If the type is not valid, we return null, which is the same as
            // 'no information'.
            return null;
        }
        return new NonNullValue(this, type);
    }
}
