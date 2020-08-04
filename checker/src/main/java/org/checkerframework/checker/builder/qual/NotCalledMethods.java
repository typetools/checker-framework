package org.checkerframework.checker.builder.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation speculatively used by Lombok's lombok.config checkerframework = true option. It has
 * no meaning to the Called Methods Checker, which treats it as {@link
 * org.checkerframework.checker.calledmethods.qual.CalledMethods}.
 *
 * <p>A similar annotation might be supported in the future.
 *
 * <p>This annotation could be marked as deprecated, but that causes extra warnings when processing
 * delombok'd code.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface NotCalledMethods {
    String[] value();
}
