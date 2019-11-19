package org.checkerframework.checker.signature.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The format produced by the {@link Class#getSimpleName()} method. It is an identifier or the empty
 * string (for an anonymous class), followed by an number of array square brackets.
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@SubtypeOf(SignatureUnknown.class)
@QualifierForLiterals(stringPatterns = "^(|[A-Za-z_][A-Za-z_0-9]*)(\\[\\])*$")
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface ClassGetSimpleName {}
