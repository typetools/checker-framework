package org.checkerframework.checker.determinism.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Changes an {@code @}{@link OrderNonDet} type into {@code @}{@link Det}.
 *
 * <p>This annotation works together with polymorphic qualifiers. When written together with a
 * polymorphic qualifier that be instantiated as {@code @OrderNonDet}, it is instead instantiated as
 * {@code @Det}.
 *
 * @checker_framework.manual #determinism-checker Determinism Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Ond2D {}
