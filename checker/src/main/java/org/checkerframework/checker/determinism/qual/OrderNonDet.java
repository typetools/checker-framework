package org.checkerframework.checker.determinism.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An expression of type {@code @OrderNonDet} evaluates to a collection (i.e any class that is a
 * subtype of java.util.Collection or java.util.Iterator) or an array containing the same values
 * (with respect to .equals()) on all executions. However, the iteration order may differ across
 * executions. Non-collections and non-arrays may not be annotated with this type qualifier.
 *
 * @checker_framework.manual #determinism-checker Determinism Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({NonDet.class})
public @interface OrderNonDet {}
