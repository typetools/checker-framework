package org.checkerframework.checker.signature.qual;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is a string that is a valid {@linkplain
 * org.checkerframework.checker.signature.qual.CanonicalName canonical name} and a valid {@linkplain
 * org.checkerframework.checker.signature.qual.BinaryName binary name}. It represents a non-array,
 * non-inner, non-primitive class.
 *
 * <p>Examples: int, MyClass, java.lang, java.lang.Integer
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({CanonicalName.class, DotSeparatedIdentifiers.class})
public @interface CanonicalNameAndBinaryName {}
