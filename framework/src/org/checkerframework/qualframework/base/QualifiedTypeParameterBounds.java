package org.checkerframework.qualframework.base;

/**
 * A pair of the qualified upper and lower bound types for a type parameter.
 * Instances of this class are immutable.
 */
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
