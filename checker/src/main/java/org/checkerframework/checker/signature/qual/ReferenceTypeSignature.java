package org.checkerframework.checker.signature.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Represents a JavaTypeSignature for a reference (non-primitive) type.
 *
 * <p>See <a href=https://docs.oracle.com/javase/specs/jvms/se10/html/jvms-4.html#jvms-4.7.9.1>JVM
 * specs</a>.
 *
 * @see GenericSignature
 * @see JavaTypeSignature
 */
@SubtypeOf(JavaTypeSignature.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface ReferenceTypeSignature {}
