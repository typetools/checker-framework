package org.checkerframework.qualframework.base;

import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedDeclaredType;

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
    public boolean isSubtype(QualifiedTypeMirror<Q> subtype, QualifiedTypeMirror<Q> supertype) {
        return adapter.superIsSubtype(subtype, supertype);
    }

    /**
     * Checks that <code>subtype</code> is a subtype of <code>supertype</code>, as an array
     * component type.
     */
    protected boolean isSubtypeAsArrayComponent(QualifiedTypeMirror<Q> subtype, QualifiedTypeMirror<Q> supertype) {
        return adapter.superIsSubtypeAsArrayComponent(subtype, supertype);
    }

    /**
     * Checks that <code>subtype</code> is a subtype of <code>supertype</code>, as actual
     * type arguments.
     */
    protected boolean isSubtypeAsTypeArgument(QualifiedTypeMirror<Q> subtype, QualifiedTypeMirror<Q> supertype) {
        return adapter.superIsSubtypeAsTypeArgument(subtype, supertype);
    }

    /**
     * Checks that <code>subtype</code> is a subtype of <code>supertype</code>.
     */
    final protected boolean isSubtypeImpl(QualifiedTypeMirror<Q> subtype, QualifiedTypeMirror<Q> supertype) {
        return adapter.superIsSubtypeImpl(subtype, supertype);
    }

    /**
     * Checks that <code>subtype</code> is a subtype of <code>supertype</code>,
     * with respect to type arguments only.   Returns true if any of the
     * provided types is not a parameterized type.  A parameterized
     * type, rhs, is a subtype of another, lhs, only if their actual type
     * parameters are invariant. 
     *
     * The arguments are declared as type {@link QualifiedTypeMirror} because
     * they may be either {@link QualifiedDeclaredType} or {@link
     * QualifiedTypeDeclaration}.
     */
    protected boolean isSubtypeTypeArguments(QualifiedTypeMirror<Q> subtype, QualifiedTypeMirror<Q> supertype) {
        return adapter.superIsSubtypeTypeArguments(subtype, supertype);
    }
}
