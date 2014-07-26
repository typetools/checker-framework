package org.checkerframework.qualframework.util;

import java.util.List;

/** {@link ExtendedTypeMirror} variant for {@link javax.lang.model.type.IntersectionType}. */
public interface ExtendedIntersectionType extends ExtendedTypeMirror {
    /** Return the bounds comprising this intersection type. */
    List<? extends ExtendedTypeMirror> getBounds();
}
