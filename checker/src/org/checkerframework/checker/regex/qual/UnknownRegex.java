package org.checkerframework.checker.regex.qual;

import java.lang.annotation.Target;

import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * Represents the top of the Regex qualifier hierarchy.
 *
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the checker.
 *
 * @checker_framework.manual #regex-checker Regex Checker
 */
@TypeQualifier
@InvisibleQualifier
@DefaultQualifierInHierarchy
@SubtypeOf({})
@Target({}) // empty target prevents programmers from writing this in a program
public @interface UnknownRegex {}
