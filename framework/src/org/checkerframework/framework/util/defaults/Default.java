package org.checkerframework.framework.util.defaults;

import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.javacutil.AnnotationUtils;

import javax.lang.model.element.AnnotationMirror;

/**
 * Represents a mapping from an Annotation to a TypeUseLocation it should be applied to during defaulting.
 * The Comparable ordering of this class first tests location then tests annotation ordering (via
 * {@link org.checkerframework.javacutil.AnnotationUtils}).
 *
 * It also has a handy toString method that is useful for debugging.
 */
class Default implements Comparable<Default> {
    // please remember to add any fields to the hashcode calculation
    public final AnnotationMirror anno;
    public final TypeUseLocation location;

    public Default(final AnnotationMirror anno, final TypeUseLocation location) {
        this.anno     = anno;
        this.location = location;
    }

    @Override
    public int compareTo(Default other) {
        int locationOrder = location.compareTo(other.location);
        if (locationOrder == 0) {
            return AnnotationUtils.annotationOrdering().compare(anno, other.anno);
        } else {
            return locationOrder;
        }
    }

    @Override
    public boolean equals(Object thatObj) {
        if (thatObj == this) {
            return true;
        }

        if (thatObj == null || !thatObj.getClass().equals(Default.class)) {
            return false;
        }

        return compareTo((Default) thatObj) == 0;
    }

    @Override
    public int hashCode() {
        return 13 + (anno == null     ? 0 : 37 * anno.hashCode())
                  + (location == null ? 0 : 41 * location.hashCode());

    }

    @Override
    public String toString() {
        return "( " + location.name() + " => " + anno + " )";
    }
}
