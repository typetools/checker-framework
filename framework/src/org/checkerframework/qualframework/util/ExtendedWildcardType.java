package org.checkerframework.qualframework.util;

/** {@link ExtendedTypeMirror} variant for {@link javax.lang.model.type.WildcardType}. */
public interface ExtendedWildcardType extends ExtendedTypeMirror {
    /**
     * Returns the upper bound of this wildcard.  Unlike
     * {@code WildcardType.getExtendsBound}, if no upper bound is
     * explicitly declared, a {@link ExtendedTypeMirror} representing
     * {@code java.lang.Object} is returned.
     */
    ExtendedTypeMirror getExtendsBound();

    /**
     * Returns the lower bound of this wildcard.  Unlike
     * {@code WildcardType.getSuperBound}, if no lower
     * bound is explicitly declared, a {@link ExtendedTypeMirror} representing
     * the type of {@code null} is returned.
     */
    ExtendedTypeMirror getSuperBound();
}
