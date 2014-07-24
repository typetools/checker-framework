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
     * Returns the original {@link TypeMirror} representation of the type, if
     * possible.  This method may return null if no {@link TypeMirror}
     * representation is available.
     */
    TypeMirror getOriginalType();

    /** Returns the kind of this type. */
    TypeKind getKind();

    /** Applies an {@link ExtendedTypeVisitor} to this object. */
    <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p);

    /** Returns true iff this {@link ExtendedTypeMirror} represents a type
     * declaration, rather than a use of a type.  This can happen only because
     * the underlying annotation-based framework uses {@link
     * org.checkerframework.framework.type.AnnotatedTypeMirror}s for both
     * declarations and uses of types.  Once the framework is fixed to no
     * longer mix the two, this method will be removed.
     */
    boolean isDeclaration();
}
