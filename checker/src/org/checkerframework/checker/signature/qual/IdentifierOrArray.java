package org.checkerframework.checker.signature.qual;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An identifier, 
 * followed by any number of array square brackets.
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@SubtypeOf({SourceNameForNonInner.class, InternalForm.class, ClassGetSimpleName.class})
@ImplicitFor(stringPatterns="^([A-Za-z_][A-Za-z_0-9]*)(\\[\\])*$")
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface IdentifierOrArray {}
