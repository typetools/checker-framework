package org.checkerframework.checker.signature.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * A class signature encodes type information about a (possibly generic) class declaration. It
 * describes any type parameters of the class, and lists its (possibly parameterized) direct
 * superclass and direct superinterfaces, if any. A type parameter is described by its name,
 * followed by any class bound and interface bounds.
 *
 * @see GenericSignature
 */
@SubtypeOf(GenericSignatureWithTypeParameters.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface ClassSignature {}
