package org.checkerframework.checker.signature.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * A source name is a string that is a valid {@linkplain
 * org.checkerframework.checker.signature.qual.FullyQualifiedName fully qualified name} and a valid
 * {@linkplain org.checkerframework.checker.signature.qual.BinaryName binary name}.
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@SubtypeOf({FullyQualifiedName.class, BinaryName.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface SourceNameForNonInner {}
