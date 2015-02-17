package org.checkerframework.checker.formatter.qual;

import java.lang.annotation.Target;

import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * The top qualifier.
 *
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the checker.
 *
 * @checker_framework.manual #formatter-checker Format String Checker
 */
@TypeQualifier
@InvisibleQualifier
@SubtypeOf({})
@DefaultQualifierInHierarchy
@Target({})
public @interface UnknownFormat {}
