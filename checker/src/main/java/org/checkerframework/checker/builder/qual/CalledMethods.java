package org.checkerframework.checker.builder.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * For compatibility with Lombok, retain the old annotations it generates.
 *
 * <p>You should use {@link org.checkerframework.checker.calledmethods.qual.CalledMethods} instead.
 * The Called Methods Checker treats this annotation as identical to that one.
 *
 * <p>This annotation could be marked as deprecated, but that causes extra warnings when processing
 * delombok'd code.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface CalledMethods {
    /**
     * The names of the methods that have been called.
     *
     * @return the names of the methods that have been called.
     */
    String[] value();
}
