package org.checkerframework.checker.signature.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Represents the signature of an array reference type as defined in <a
 * href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.9.1">Java Virtual
 * Machine Specification, section 4.3.2</a>.
 *
 * For example:
 * 
 * {@code [[Lpkg/Cl<+Base>;}
 *
 * @see GenericSignature
 */
@SubtypeOf(ReferenceTypeSignature.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface ArrayTypeSignature {}
