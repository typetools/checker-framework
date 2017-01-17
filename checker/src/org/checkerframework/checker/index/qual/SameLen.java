package org.checkerframework.checker.index.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.*;

/**
 * This type annotation indicates that an expression's value is an array that has the same length as
 * the other arrays listed in value field.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SubtypeOf(SameLenUnknown.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface SameLen {
    /** A list of other arrays with the same length. */
    String[] value();
}
