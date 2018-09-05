package org.checkerframework.checker.signature.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Represents the signature of a generic method as defined in <a
 * href=https://docs.oracle.com/javase/specs/jvms/se10/html/jvms-4.html#jvms-4.7.9.1>JVM specs</a>
 *
 * @see GenericSignature
 */
@SubtypeOf(GenericSignatureWithTypeParameters.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface MethodSignature {}
