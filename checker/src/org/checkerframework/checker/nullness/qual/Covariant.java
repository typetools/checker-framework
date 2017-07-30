package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Deprecated! See {@link org.checkerframework.framework.qual.Covariant} instead.
 *
 * @checker_framework.manual #covariant-type-parameters Covariant type parameters
 */
@Deprecated
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Covariant {
    int[] value();
}
