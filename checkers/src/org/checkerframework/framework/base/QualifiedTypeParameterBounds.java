package org.checkerframework.framework.base;

public class QualifiedTypeParameterBounds<Q> {
    private final QualifiedTypeMirror<Q> upperBound;
    private final QualifiedTypeMirror<Q> lowerBound;

    public QualifiedTypeParameterBounds(
            QualifiedTypeMirror<Q> upperBound,
            QualifiedTypeMirror<Q> lowerBound) {
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
    }

    public QualifiedTypeMirror<Q> getUpperBound() {
        return upperBound;
    }

    public QualifiedTypeMirror<Q> getLowerBound() {
        return lowerBound;
    }
}
