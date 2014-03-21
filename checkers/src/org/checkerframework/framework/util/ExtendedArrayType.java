package org.checkerframework.framework.util;

/** {@link ExtendedTypeMirror} variant for {@link javax.lang.model.type.ArrayType}. */
public interface ExtendedArrayType extends ExtendedReferenceType {
    /** Returns the component type of this array type. */
    ExtendedTypeMirror getComponentType();
}

