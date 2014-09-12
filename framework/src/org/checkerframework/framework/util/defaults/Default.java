package org.checkerframework.framework.util.defaults;

import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.javacutil.AnnotationUtils;

import javax.lang.model.element.AnnotationMirror;

class Default implements Comparable<Default> {
    public final AnnotationMirror anno;
    public final DefaultLocation location;

    public Default(final AnnotationMirror anno, final DefaultLocation location) {
        this.anno     = anno;
        this.location = location;
    }

    @Override
    public int compareTo(Default other) {
        int locationOrder = location.compareTo(other.location);
        if( locationOrder == 0 ) {
            return AnnotationUtils.annotationOrdering().compare(anno, other.anno);
        } else {
            return locationOrder;
        }
    }

    @Override
    public boolean equals(Object thatObj) {
        if(thatObj == this) {
            return true;
        }

        if(thatObj == null || !thatObj.getClass().equals(Default.class)) {
            return false;
        }

        return compareTo((Default) thatObj) == 0;
    }

    @Override
    public int hashCode() {
        return 13 + (anno == null     ? 0 : 37 * anno.hashCode())
                + (location == null ? 0 : 37 * location.hashCode());

    }

    @Override
    public String toString() {
        return "( " + location.name() + " => " + anno + " )";
    }
}
