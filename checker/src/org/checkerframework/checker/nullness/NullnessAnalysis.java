package org.checkerframework.checker.nullness;

import java.util.List;

import javax.lang.model.element.VariableElement;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.Pair;

/**
 * The analysis class for the non-null type system (serves as factory for the
 * transfer function, stores and abstract values.
 *
 * @author Stefan Heule
 */
public class NullnessAnalysis extends
        CFAbstractAnalysis<NullnessValue, NullnessStore, NullnessTransfer> {

    public NullnessAnalysis(BaseTypeChecker checker,
            NullnessAnnotatedTypeFactory factory,
            List<Pair<VariableElement, NullnessValue>> fieldValues) {
        super(checker, factory, fieldValues);
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
