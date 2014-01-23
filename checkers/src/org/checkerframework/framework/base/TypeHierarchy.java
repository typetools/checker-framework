package org.checkerframework.framework.base;

public interface TypeHierarchy<Q> {
    boolean isSubtype(QualifiedTypeMirror<Q> a, QualifiedTypeMirror<Q> b);
}
