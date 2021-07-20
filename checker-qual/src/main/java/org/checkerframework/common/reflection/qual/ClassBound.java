package org.checkerframework.common.reflection.qual;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This represents a {@code Class<T>} object whose run-time value is equal to or a subtype of one of
 * the arguments.
 *
 * @checker_framework.manual #methodval-and-classval-checkers ClassVal Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({UnknownClass.class})
public @interface ClassBound {
    /**
     * The <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-13.html#jls-13.1">binary
     * name</a> of the class or classes that upper-bound the values of this Class object.
     */
    String[] value();
}
