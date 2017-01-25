package org.checkerframework.checker.index.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.*;

/**
 * The value of the annotated expression is a sequence containing at least the given number of
 * elements.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@DefaultQualifierInHierarchy
@SubtypeOf({})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface MinLen {
    /** The minimum number of elements in this sequence. */
    int value() default 0;
}
