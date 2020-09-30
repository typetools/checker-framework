package org.checkerframework.common.value.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation indicating the possible values for an enum type. If an expression's type has this
 * annotation, then at run time, the expression evaluates to one of the enum values named by the
 * arguments. EnumVal uses the simple name of the enum value: the EnumVal type corresponding to
 * {@code MyEnum.MY_VALUE} is {@code {@literal @}EnumVal("MY_VALUE")}.
 *
 * <p>This annotation is treated as {@link StringVal} internally by the Constant Value Checker.
 *
 * @checker_framework.manual #constant-value-checker Constant Value Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
public @interface EnumVal {
    /**
     * The names of the possible enum values the annotated type might contain.
     *
     * @return the names
     */
    String[] value() default {};
}
