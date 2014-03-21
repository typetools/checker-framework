package org.checkerframework.framework.base;

import org.checkerframework.framework.base.QualifiedTypeMirror.QualifiedDeclaredType;

/** Default implementation of {@link TypeHierarchy}.  Applies standard Java
 * subtyping rules to compare Java types, and uses a {@link QualifierHierarchy}
 * to compare qualifiers.
 */
public class DefaultTypeHierarchy<Q> implements TypeHierarchy<Q> {
    private QualifierHierarchy<Q> qualifierHierarchy;
    private TypeHierarchyAdapter<Q> adapter;

    /**
     * @param qualifierHierarchy   
     *      a {@link QualifierHierarchy} to use for comparing individual qualifiers
     */
    public DefaultTypeHierarchy(QualifierHierarchy<Q> qualifierHierarchy) {
        this.qualifierHierarchy = qualifierHierarchy;
    }

    void setAdapter(TypeHierarchyAdapter<Q> adapter) {
        this.adapter = adapter;
    }


    @Override
    public boolean isSubtype(QualifiedTypeMirror<Q> a, QualifiedTypeMirror<Q> b) {
        return adapter.superIsSubtype(a, b);
    }

    /**
     * Checks that <code>a</code> is a subtype of <code>b</code>, as an array
     * component type.
     */
    protected boolean isSubtypeAsArrayComponent(QualifiedTypeMirror<Q> a, QualifiedTypeMirror<Q> b) {
        return adapter.superIsSubtypeAsArrayComponent(a,b);
    }

    /**
     * Checks that <code>a</code> is a subtype of <code>b</code>, as actual
     * type arguments.
     */
    protected boolean isSubtypeAsTypeArgument(QualifiedTypeMirror<Q> a, QualifiedTypeMirror<Q> b) {
        return adapter.superIsSubtypeAsTypeArgument(a, b);
    }

    /**
     * Checks that <code>a</code> is a subtype of <code>b</code>.
     */
    final protected boolean isSubtypeImpl(QualifiedTypeMirror<Q> a, QualifiedTypeMirror<Q> b) {
        return adapter.superIsSubtypeImpl(a, b);
    }

    /**
     * Checks that <code>a</code> is a subtype of <code>b</code>, with respect
     * to type arguments only.   Returns true if any of the provided types is
     * not a parameterized type.  A parameterized type, rhs, is a subtype of
     * another, lhs, only if their actual type parameters are invariant. 
     */
    protected boolean isSubtypeTypeArguments(QualifiedDeclaredType<Q> a, QualifiedDeclaredType<Q> b) {
        return adapter.superIsSubtypeTypeArguments(a, b);
    }
}
