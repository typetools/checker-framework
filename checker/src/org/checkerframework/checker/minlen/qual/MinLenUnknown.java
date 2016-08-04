package org.checkerframework.checker.minlen.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * In the MinLen Checker's type system, this type
 * represents any array with unknown length
 *
 * @checker_framework.manual #minlen MinLen Checker
 */
@DefaultQualifierInHierarchy
@SubtypeOf({})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface MinLenUnknown {}
