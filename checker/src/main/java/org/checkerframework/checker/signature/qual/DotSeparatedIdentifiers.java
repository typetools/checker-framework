package org.checkerframework.checker.signature.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * This is a string that is a valid {@linkplain
 * org.checkerframework.checker.signature.qual.FullyQualifiedName fully qualified name} and a valid
 * {@linkplain org.checkerframework.checker.signature.qual.BinaryName binary name}. It represents a
 * non-array, non-inner class: dot-separated identifiers.
 *
 * <p>Examples: int, MyClass, java.lang.Integer
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@SubtypeOf({FullyQualifiedName.class, BinaryName.class})
@QualifierForLiterals(stringPatterns = "^[A-Za-z_][A-Za-z_0-9]*(\\.[A-Za-z_][A-Za-z_0-9]*)*$")
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface DotSeparatedIdentifiers {}
