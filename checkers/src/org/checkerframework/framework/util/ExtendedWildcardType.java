package org.checkerframework.framework.util;

public interface ExtendedWildcardType extends ExtendedTypeMirror {
    ExtendedTypeMirror getExtendsBound();
    ExtendedTypeMirror getSuperBound();
}
