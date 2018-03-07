package org.checkerframework.checker.nondeterminism.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Indicates that the collection has the same elements in possibly different orders across
 * executions. Non-collections cannot be annotated with this type.
 *
 * @checker_framework.manual #nondeterminism-checker NonDeterminism Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({ValueNonDet.class})
public @interface OrderNonDet {}
