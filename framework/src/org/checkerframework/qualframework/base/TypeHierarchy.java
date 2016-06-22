package org.checkerframework.qualframework.base;

/**
 * {@link DefaultQualifiedTypeFactory} component for performing subtyping
 * checks between {@link QualifiedTypeMirror}s.
 */
public interface TypeHierarchy<Q> {
    /** Checks if {@code subtype} is a subtype of {@code supertype}. */
    boolean isSubtype(QualifiedTypeMirror<Q> subtype, QualifiedTypeMirror<Q> supertype);
}
