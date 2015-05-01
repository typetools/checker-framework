package org.checkerframework.checker.javari.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates the annotated {@code Field} may be re-assigned regardless of the
 * immutability of the enclosing class or object instance.
 *
 * <p>
 *
 * This annotation is part of the Javari language.
 *
 * @checker_framework.manual #javari-checker Javari Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Assignable {}
