package org.checkerframework.checker.nullness.compatqual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identical to {@code @KeyFor}. It appears in the same package as {@code @KeyForDecl}, so it is
 * convenient to import both.
 *
 * @see org.checkerframework.checker.nullness.qual.KeyFor
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface KeyForType {
    public String[] value();
}
