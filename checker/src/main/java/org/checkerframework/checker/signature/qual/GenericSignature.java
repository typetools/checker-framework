package org.checkerframework.checker.signature.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Represents a generic type signature as defined in <a
 * href=https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.9.1>JVM specs</a>.
 * Can be one of the following:
 *
 * <p>1. JavaTypeSignature JavaTypeSignature of type {@code GenericClass<? extends cl>}:
 * Lorg/pkg/GenericClass<+Lorg/pkg/cl>
 *
 * <p>JavaTypeSignature of type {@code GenericClass<? extends cl>[]}:
 * [Lorg/pkg/GenericClass<+Lorg/pkg/cl>
 *
 * <p>JavaTypeSignature of a type variable named R: TR;
 *
 * <p>2. ClassSignature <R:Object:Runnable>Lorg/pkg/GenericClass<TR;>;
 *
 * <p>ClassSignature of class: {@code class T<R extends Runnable> extends GenericClass<R>}
 *
 * <p>3. MethodSignature <R::Runnable>(TR;Ljava/lang/String;)V^Ljava/lang/Exception;
 */
@SubtypeOf(SignatureUnknown.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface GenericSignature {}
