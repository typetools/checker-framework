package org.checkerframework.checker.signature.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * A JavaTypeSignature represents either a reference type or a primitive type of the Java
 * programming language.
 *
 * <p>It might be: 1. Descriptor of a primitive {@code [BCDFIJSZ] } 2. A TypeVariable {@code TK;} 3.
 * Signature of a generic reference type {@code Ljava/util/ArrayList<-LCl;>; } 4. Array of any of
 * the above {@code [[D}
 *
 * @see GenericSignature
 *     <p>see <a
 *     href=https://docs.oracle.com/javase/specs/jvms/se10/html/jvms-4.html#jvms-4.7.9.1>JVM
 *     Specifications section 4.9.1</a>
 */
@SubtypeOf(GenericSignature.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface JavaTypeSignature {}
