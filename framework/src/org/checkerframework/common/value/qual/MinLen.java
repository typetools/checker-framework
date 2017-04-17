package org.checkerframework.common.value.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * The value of the annotated expression is a sequence containing at least the given number of
 * elements. An alias for an {@link ArrayLenRange} annotation with only a {@code from} field.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface MinLen {
    /** The minimum number of elements in this sequence. */
    int value() default 0;
}
