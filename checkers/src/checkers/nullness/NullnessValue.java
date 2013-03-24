package checkers.nullness;

import checkers.flow.analysis.checkers.CFAbstractAnalysis;
import checkers.flow.analysis.checkers.CFAbstractValue;
import checkers.flow.analysis.checkers.CFValue;
import checkers.nullness.quals.Nullable;
import checkers.nullness.quals.PolyNull;
import checkers.types.AnnotatedTypeMirror;

/**
 * Behaves just like {@link CFValue}, but additionally tracks whether at this
 * point {@link PolyNull} is known to be {@link Nullable}.
 *
 * @author Stefan Heule
 */
public class NullnessValue extends CFAbstractValue<NullnessValue> {

    protected boolean isPolyNullNull;

    public NullnessValue(CFAbstractAnalysis<NullnessValue, ?, ?> analysis,
            AnnotatedTypeMirror type) {
        super(analysis, type);
    }

}
