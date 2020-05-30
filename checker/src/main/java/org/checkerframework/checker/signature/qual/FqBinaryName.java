package org.checkerframework.checker.signature.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An extension of binary name format to represent primitives and arrays. It is just like
 * fully-qualified name format, but uses "$" rather than "." to indicate a nested class.
 *
 * <p>Examples include
 *
 * <pre>
 *   int
 *   int[][]
 *   java.lang.String
 *   java.lang.String[]
 *   pkg.Outer$Inner
 *   pkg.Outer$Inner[]
 * </pre>
 *
 * <p>This is not a format defined by the Java language or platform, but is a convenient format for
 * users to unambiguously specify a type.
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(SignatureUnknown.class)
public @interface FqBinaryName {}
