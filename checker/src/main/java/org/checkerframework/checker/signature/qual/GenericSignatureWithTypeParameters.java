package org.checkerframework.checker.signature.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Represents a generic signature which may define type variables(signatures which represent a
 * declaration), i.e. MethodSignature and ClassSignature
 *
 * @see GenericSignature
 * @see MethodSignature
 * @see ClassSignature
 */
@SubtypeOf(GenericSignature.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface GenericSignatureWithTypeParameters {}
