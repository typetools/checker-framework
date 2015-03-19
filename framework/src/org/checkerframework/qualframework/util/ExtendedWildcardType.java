package org.checkerframework.qualframework.util;

/** {@link ExtendedTypeMirror} variant for {@link javax.lang.model.type.WildcardType}. */
public interface ExtendedWildcardType extends ExtendedTypeMirror {
    /**
     * Returns the upper bound of this wildcard.  Unlike
     * <code>WildcardType.getExtendsBound</code>, if no upper bound is
     * explicitly declared, a {@link ExtendedTypeMirror} representing
     * <code>java.lang.Object</code> is returned.
     */
    ExtendedTypeMirror getExtendsBound();

    /**
     * Returns the lower bound of this wildcard.  Unlike
     * <code>WildcardType.getSuperBound</code>, if no lower
     * bound is explicitly declared, a {@link ExtendedTypeMirror} representing
     * the type of <code>null</code> is returned.
     */
    ExtendedTypeMirror getSuperBound();
}
