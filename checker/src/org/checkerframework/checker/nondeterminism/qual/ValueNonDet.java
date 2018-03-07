package org.checkerframework.checker.nondeterminism.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Collections : No guarantee on the order or the values of elements. Non-collections : Can have
 * different values across executions.
 *
 * @checker_framework.manual #nondeterminism-checker NonDeterminism Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
public @interface ValueNonDet {}
