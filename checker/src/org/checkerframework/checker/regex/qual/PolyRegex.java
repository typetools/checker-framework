package org.checkerframework.checker.regex.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.qualframework.poly.qual.DefaultValue;

/**
 * A polymorphic qualifier for the Regex type system.
 *
 * <p>
 * Any method written using {@code @PolyRegex} conceptually has two versions:
 * one in which every instance of {@code @PolyRegex String} has been replaced
 * by {@code @Regex String}, and one in which every instance of
 * {@code @PolyRegex String} has been replaced by {@code String}.
 *
 * @checker_framework.manual #regex-checker Regex Checker
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Repeatable(MultiPolyRegex.class)
public @interface PolyRegex {
    /**
     * The name of the qualifier parameter to set.
     */
    String param() default DefaultValue.PRIMARY_TARGET;
}
