package org.checkerframework.common.value.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An annotation indicating the possible values for a bool type. If an expression's type has this
 * annotation, then at run time, the expression evaluates to one of the annotation's arguments.
 *
 * @checker_framework.manual #constant-value-checker Constant Value Checker
 */
@SubtypeOf({UnknownVal.class})
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
public @interface BoolVal {
    boolean[] value();
}
