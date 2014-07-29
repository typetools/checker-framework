package org.checkerframework.qualframework.util;

import java.util.List;
import javax.lang.model.element.ExecutableElement;

/** {@link ExtendedTypeMirror} variant for {@link javax.lang.model.type.ExecutableType}. */
public interface ExtendedExecutableType extends ExtendedTypeMirror {
    /** Returns the element corresponding to this type. */
    ExecutableElement asElement();

    /** Returns the types of this executable's formal parameters. */
    List<? extends ExtendedTypeMirror> getParameterTypes();

    /**
     * Returns the receiver type of this executable, or {@link ExtendedNoType}
     * with kind <code>NONE</code> if the executable has no receiver type. An
     * executable which is an instance method, or a constructor of an inner
     * class, has a receiver type derived from the declaring type. An
     * executable which is a static method, or a constructor of a non-inner
     * class, or an initializer (static or instance), has no receiver type.
     */
    ExtendedTypeMirror getReceiverType();

    /**
     * Returns the return type of this executable. Returns an {@link
     * ExtendedNoType} with kind <code>VOID</code> if this executable is a
     * method that does not return a value.  Unlike
     * <code>ExecutableType.getReturnType</code>, calling this method on the
     * type of a constructor returns the type of the object to be constructed.
     */
    ExtendedTypeMirror getReturnType();

    /** Returns the exceptions and other throwables listed in this executable's
     * <code>throws</code> clause. */
    List<? extends ExtendedTypeMirror> getThrownTypes();

    /** Returns the type variables declared by the formal type parameters of
     * this executable. */
    List<? extends ExtendedParameterDeclaration> getTypeParameters();
}
