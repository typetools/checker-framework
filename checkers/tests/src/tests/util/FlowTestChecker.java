package tests.util;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.Bottom;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;

@TypeQualifiers( { Value.class, Odd.class, MonotonicOdd.class, Unqualified.class, Bottom.class } )
public final class FlowTestChecker extends BaseTypeChecker {
    /*
    @Override
    public void initChecker() {
        super.initChecker();
    }
    */
}
