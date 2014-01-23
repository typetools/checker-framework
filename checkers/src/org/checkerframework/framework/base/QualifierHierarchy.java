package org.checkerframework.framework.base;

public interface QualifierHierarchy<Q> {
    boolean isSubtype(Q a, Q b);
    Q leastUpperBound(Q a, Q b);
    Q greatestLowerBound(Q a, Q b);
    Q getTop();
    Q getBottom();
}
