package org.checkerframework.checker.upperbound.qual;

import java.lang.annotation.*;
import org.checkerframework.framework.qual.*;

/**
 * Indicates that the expression annotated with this type qualifier is less than or equal to the
 * length of the listed arrays.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SubtypeOf(UpperBoundUnknown.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface LessThanOrEqualToLength {
    public String[] value();
}
