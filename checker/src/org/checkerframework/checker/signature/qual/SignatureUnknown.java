package org.checkerframework.checker.signature.qual;

import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Top qualifier in the type hierarchy.
 *
 * <p>This annotation may not be written in source code; it is an implementation detail of the
 * checker.
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@DefaultQualifierInHierarchy
@SubtypeOf({})
@Target({}) // empty target prevents programmers from writing this in a program
public @interface SignatureUnknown {}
