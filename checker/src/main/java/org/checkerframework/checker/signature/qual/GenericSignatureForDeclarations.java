package org.checkerframework.checker.signature.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * A signature that represents a class or method or declaration.
 *
 * @see GenericSignature
 * @see MethodSignature
 * @see ClassSignature
 */
@SubtypeOf(GenericSignature.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface GenericSignatureForDeclarations {}
