package org.checkerframework.qualframework.util;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Represents a type in the Java programming language.  The {@link
 * ExtendedTypeMirror} hierarchy follows the same structure as the standard
 * {@link TypeMirror} hierarchy.  We use these <code>Extended</code> interfaces
 * instead of the standard {@link TypeMirror} interfaces because in some cases
 * we require behavior that differs from the specification of {@link
 * TypeMirror}.  (See the subinterfaces' documentation for details.)
 */
public interface ExtendedTypeMirror extends AnnotatedConstruct {
    /**
     * Returns the same type represented as a standard {@link TypeMirror}, if
     * possible.  This method may return null if no {@link TypeMirror}
     * representation is available.
     */
    TypeMirror getRaw();

    /** Returns the kind of this type. */
    TypeKind getKind();

    /** Applies an {@link ExtendedTypeVisitor} to this object. */
    <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p);
}
