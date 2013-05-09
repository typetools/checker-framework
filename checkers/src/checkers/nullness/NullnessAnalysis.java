package checkers.nullness;

import java.util.List;

import javacutils.Pair;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;

import checkers.flow.CFAbstractAnalysis;
import checkers.types.AnnotatedTypeMirror;
import checkers.util.AnnotatedTypes;

/**
 * The analysis class for the non-null type system (serves as factory for the
 * transfer function, stores and abstract values.
 *
 * @author Stefan Heule
 */
public class NullnessAnalysis extends
        CFAbstractAnalysis<NullnessValue, NullnessStore, NullnessTransfer> {

    public NullnessAnalysis(NullnessAnnotatedTypeFactory factory,
            ProcessingEnvironment env, AbstractNullnessChecker checker,
            List<Pair<VariableElement, NullnessValue>> fieldValues) {
        super(factory, env, checker, fieldValues);
    }

    @Override
    public NullnessStore createEmptyStore(boolean sequentialSemantics) {
        return new NullnessStore(this, sequentialSemantics);
    }

    @Override
    public NullnessStore createCopiedStore(NullnessStore s) {
        return new NullnessStore(s);
    }

    @Override
    public NullnessValue createAbstractValue(AnnotatedTypeMirror type) {
        if (!AnnotatedTypes.isValidType(qualifierHierarchy, type)) {
            // If the type is not valid, we return null, which is the same as
            // 'no information'.
            return null;
        }
        return new NullnessValue(this, type);
    }
}
