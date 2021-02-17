package org.checkerframework.checker.signature.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * This is a string that is a valid {@linkplain
 * org.checkerframework.checker.signature.qual.CanonicalName canonical name} and a valid {@linkplain
 * org.checkerframework.checker.signature.qual.BinaryNameOrPrimitiveType binary name or primitive
 * type}. It represents a primitive type or a non-array, non-inner class, where the latter is
 * represented by dot-separated identifiers.
 *
 * <p>Examples: int, MyClass, java.lang.Integer
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({DotSeparatedIdentifiersOrPrimitiveType.class})
public @interface CanonicalNameOrPrimitiveType {}
