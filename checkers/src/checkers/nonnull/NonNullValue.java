package checkers.nonnull;

import checkers.flow.analysis.checkers.CFAbstractAnalysis;
import checkers.flow.analysis.checkers.CFAbstractValue;
import checkers.types.AnnotatedTypeMirror;

public class NonNullValue extends CFAbstractValue<NonNullValue> {

    protected boolean isPolyNullNull;

    public NonNullValue(CFAbstractAnalysis<NonNullValue, ?, ?> analysis,
            AnnotatedTypeMirror type) {
        super(analysis, type);
    }

}
