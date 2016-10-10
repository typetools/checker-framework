package org.checkerframework.checker.minlen.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.*;

/**
 * This type annotation indicates that an expression's value is
 * an array containing at least the given number of elements.
 * MinLen(0) is the top type for the MinLen type system,
 * and in general MinLen(x) is a subtype of MinLen(x-1).
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@DefaultQualifierInHierarchy
@SubtypeOf({})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface MinLen {
    /** The minimum number of elements in this array. */
    int value();
}
