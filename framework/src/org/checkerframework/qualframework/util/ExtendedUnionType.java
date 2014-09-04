package org.checkerframework.qualframework.util;

import java.util.List;

/** {@link ExtendedTypeMirror} variant for {@link javax.lang.model.type.UnionType}. */
public interface ExtendedUnionType extends ExtendedTypeMirror {
    /** Return the alternatives comprising this union type. */
    List<? extends ExtendedTypeMirror> getAlternatives();
}
