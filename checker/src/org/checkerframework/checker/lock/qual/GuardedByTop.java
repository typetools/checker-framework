package org.checkerframework.checker.lock.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * The top of the guarded-by qualifier hierarchy.
 * <p>
 *
 * @checker_framework_manual #lock-checker Lock Checker
 */
@TypeQualifier
@SubtypeOf({})
@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface GuardedByTop {}