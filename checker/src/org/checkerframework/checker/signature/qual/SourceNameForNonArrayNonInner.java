package org.checkerframework.checker.signature.qual;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * A source name is a string that is a valid {@linkplain FullyQualifiedName
 * fully qualified name} and a valid {@linkplain BinaryName binary name}.
 * It represents a non-array, non-inner class: dot-separated identifiers.
 *
 * Example: int
 * Example: MyClass
 * Example: java.lang.Integer
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@SubtypeOf({SourceNameForNonInner.class, BinaryNameForNonArray.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface SourceNameForNonArrayNonInner {}
