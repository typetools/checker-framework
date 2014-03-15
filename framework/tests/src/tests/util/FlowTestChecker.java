package tests.util;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.Bottom;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.framework.qual.Unqualified;

@TypeQualifiers( { Value.class, Odd.class, MonotonicOdd.class, Unqualified.class, Bottom.class } )
public final class FlowTestChecker extends BaseTypeChecker {
    /*
    @Override
    public void initChecker() {
        super.initChecker();
    }
    */
}
