package org.checkerframework.checker.signature.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Represents a class declaration as defined in the <a
 * href="https://docs.oracle.com/javase/specs/jvms/se10/html/jvms-4.html#jvms-4.7.9.1">JVM
 * specification</a>.
 *
 * <p>An example is {@code Lfoo/bar/Baz<Lp1.Quux;>.22;}.
 *
 * @see GenericSignature
 */
@SubtypeOf(ReferenceTypeSignature.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface ClassTypeSignature {}
