package org.checkerframework.framework.base;

/**
 * {@link DefaultQualifiedTypeFactory} component for performing subtyping
 * checks between {@link QualifiedTypeMirror}s.
 */
public interface TypeHierarchy<Q> {
    /** Checks if <code>a</code> is a subtype of <code>b</code>. */
    boolean isSubtype(QualifiedTypeMirror<Q> a, QualifiedTypeMirror<Q> b);
}
