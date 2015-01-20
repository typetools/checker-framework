package org.checkerframework.checker.signature.qual;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * A source name is a string that is a valid {@linkplain FullyQualifiedName
 * fully qualified name} and a valid {@linkplain BinaryName binary name}.
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the checker.
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@TypeQualifier
@SubtypeOf({SourceName.class, BinaryNameForNonArray.class})
// A @Target meta-annotation with an empty argument would prevent programmers
// from writing this in a program, but it might sometimes be useful.
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface SourceNameForNonArray {}
