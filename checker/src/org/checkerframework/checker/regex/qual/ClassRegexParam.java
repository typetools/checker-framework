package org.checkerframework.checker.regex.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.checker.tainting.qual.ClassTaintingParam;
import org.checkerframework.qualframework.poly.qual.DefaultValue;

/**
 * ClassRegexParam declares a regex qualifier parameter on a class.
 *
 * @see ClassTaintingParam
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Repeatable(MultiClassRegexParam.class)
public @interface ClassRegexParam {
    /**
     * The name of the qualifier parameter to declare.
     */
    String value() default DefaultValue.PRIMARY_TARGET;
}
