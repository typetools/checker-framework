package org.checkerframework.checker.lock.qual;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A type that represents a newly-constructed object. It can be treated as having any
 * {@code @}{@link GuardedBy} type. Typically, it is only written on factory method return types.
 *
 * @checker_framework.manual #lock-checker Lock Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({TypeUseLocation.EXPLICIT_LOWER_BOUND, TypeUseLocation.EXPLICIT_UPPER_BOUND})
@SubtypeOf({GuardedBy.class, GuardSatisfied.class})
@DefaultFor(TypeUseLocation.CONSTRUCTOR_RESULT)
@QualifierForLiterals({LiteralKind.STRING, LiteralKind.PRIMITIVE})
public @interface NewObject {}
