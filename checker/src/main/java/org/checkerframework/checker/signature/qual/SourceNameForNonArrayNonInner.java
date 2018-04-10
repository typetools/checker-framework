package org.checkerframework.checker.signature.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * A source name is a string that is a valid {@linkplain
 * org.checkerframework.checker.signature.qual.FullyQualifiedName fully qualified name} and a valid
 * {@linkplain org.checkerframework.checker.signature.qual.BinaryName binary name}. It represents a
 * non-array, non-inner class: dot-separated identifiers.
 *
 * <p>Example: int Example: MyClass Example: java.lang.Integer
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@SubtypeOf({SourceNameForNonInner.class, BinaryNameForNonArray.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface SourceNameForNonArrayNonInner {}
