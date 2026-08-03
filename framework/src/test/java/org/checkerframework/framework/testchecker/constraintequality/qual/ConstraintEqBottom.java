package org.checkerframework.framework.testchecker.constraintequality.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/** The bottom qualifier of the ConstraintEquality Checker's hierarchy. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({ConstraintEqTop.class})
@DefaultFor({TypeUseLocation.LOWER_BOUND})
public @interface ConstraintEqBottom {}
