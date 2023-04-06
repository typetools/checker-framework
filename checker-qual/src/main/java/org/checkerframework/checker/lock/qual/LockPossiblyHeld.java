package org.checkerframework.checker.lock.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * Indicates that an expression is not known to be {@link LockHeld}.
 *
 * <p>This annotation may not be written in source code; it is an implementation detail of the
 * checker.
 *
 * @see LockHeld
 * @checker_framework.manual #lock-checker Lock Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
@InvisibleQualifier
@SubtypeOf({})
@DefaultQualifierInHierarchy
@DefaultFor(value = TypeUseLocation.LOWER_BOUND, types = Void.class)
@QualifierForLiterals(LiteralKind.NULL)
public @interface LockPossiblyHeld {}
