package org.checkerframework.framework.base;

import org.checkerframework.framework.base.QualifiedTypeMirror.QualifiedDeclaredType;

public class DefaultTypeHierarchy<Q> implements TypeHierarchy<Q> {
    private QualifierHierarchy<Q> qualifierHierarchy;
    private TypeHierarchyAdapter<Q> adapter;

    public DefaultTypeHierarchy(QualifierHierarchy<Q> qualifierHierarchy) {
        this.qualifierHierarchy = qualifierHierarchy;
    }

    void setAdapter(TypeHierarchyAdapter<Q> adapter) {
        this.adapter = adapter;
    }


    public boolean isSubtype(QualifiedTypeMirror<Q> a, QualifiedTypeMirror<Q> b) {
        return adapter.superIsSubtype(a, b);
    }

    protected boolean isSubtypeAsArrayComponent(QualifiedTypeMirror<Q> a, QualifiedTypeMirror<Q> b) {
        return adapter.superIsSubtypeAsArrayComponent(a,b);
    }

    protected boolean isSubtypeAsTypeArgument(QualifiedTypeMirror<Q> a, QualifiedTypeMirror<Q> b) {
        return adapter.superIsSubtypeAsTypeArgument(a, b);
    }

    final protected boolean isSubtypeImpl(QualifiedTypeMirror<Q> a, QualifiedTypeMirror<Q> b) {
        return adapter.superIsSubtypeImpl(a, b);
    }

    protected boolean isSubtypeTypeArguments(QualifiedDeclaredType<Q> a, QualifiedDeclaredType<Q> b) {
        return adapter.superIsSubtypeTypeArguments(a, b);
    }
}
